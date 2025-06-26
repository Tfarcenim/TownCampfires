package tfar.towncampfires.data.quest;

import com.mojang.datafixers.util.Pair;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.Lazy;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.TownCampfires;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class QuestInstance {

    private final TownCampfire campfire;
    private final ResourceLocation questID;
    private boolean active;
    private final UUID leader;
    List<UUID> members = new ArrayList<>();
    List<Integer> progress;

    net.minecraftforge.common.util.Lazy<Quest> questLazy;

    public QuestInstance(TownCampfire campfire, ResourceLocation questID, UUID leader) {
        this.campfire = campfire;
        this.questID = questID;
        this.leader = leader;
        progress = new ArrayList<>(10);
        questLazy= Lazy.of(() -> TownCampfires.questLoader.getQuestMap().get(questID));
    }

    public void toPacket(FriendlyByteBuf buf) {
        buf.writeResourceLocation(questID);
        buf.writeBoolean(active);
        buf.writeUUID(leader);
        buf.writeCollection(progress, FriendlyByteBuf::writeInt);
    }

    public static QuestInstance fromPacket(FriendlyByteBuf buf) {
        ResourceLocation questID = buf.readResourceLocation();
        boolean active = buf.readBoolean();
        UUID leader = buf.readUUID();
        List<Integer> integers = buf.readList(FriendlyByteBuf::readInt);

        QuestInstance questInstance = new QuestInstance(null,questID,leader);
        questInstance.setActive(active);
        questInstance.progress = integers;
        return questInstance;
    }

    public Quest quest() {
        return questLazy.get();
    }

    public ResourceLocation questID() {
        return questID;
    }

    public List<Integer> progress() {
        return progress;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public boolean hasPlayer(ServerPlayer player) {
        return isLeader(player) || members.contains(player.getUUID());
    }

    public boolean isLeader(ServerPlayer player) {
        return player.getUUID().equals(leader);
    }

    public void addPlayer(ServerPlayer player) {
        addPlayer(player.getUUID());
    }

    public void addPlayer(UUID uuid) {
        members.add(uuid);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("quest",questID.toString());
        tag.putString("leader",leader.toString());
        tag.putBoolean("active",active);
        ListTag listTag = new ListTag();
        for (UUID uuid : members) {
            listTag.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("members",listTag);
        return tag;
    }

    public static QuestInstance load(TownCampfire campfire,CompoundTag tag) {
        ResourceLocation questID = new ResourceLocation(tag.getString("quest"));
        UUID leader = UUID.fromString(tag.getString("leader"));
        QuestInstance questInstance = new QuestInstance(campfire,questID,leader);
        ListTag listTag = tag.getList("members", Tag.TAG_STRING);
        for (Tag tag1 : listTag) {
            questInstance.addPlayer(UUID.fromString(tag1.getAsString()));
        }
        questInstance.setActive(tag.getBoolean("active"));
        return questInstance;
    }

    public <T extends AbstractCriterionTriggerInstance> void check(SimpleCriterionTrigger<T> trigger, ServerPlayer pPlayer, Predicate<T> pTestTrigger) {
        if (!hasPlayer(pPlayer)) return;
        Quest quest = quest();
        boolean update = false;
        int criteriaCount = quest.criterias().size();
        for (int i = 0 ; i <criteriaCount;i++) {
            Pair<QuestCriteria<?>, Integer> pair = quest.criterias().get(i);
            QuestCriteria<?> questCriteria = pair.getFirst();
            CriterionTriggerInstance o = questCriteria.triggerInstance();

            if (questCriteria.trigger() == trigger) {
                if (pTestTrigger.test((T) o)) {
                    if (progress.isEmpty()) {
                        progress = NonNullList.withSize(criteriaCount,0);
                    }
                    progress.set(i, progress.get(i) + 1);
                    update = true;
                }
            }
        }
        if (update) {
            checkFinished();
            campfire.sendQuestsTo(pPlayer);
        }
    }

    void checkFinished() {

    }
}
