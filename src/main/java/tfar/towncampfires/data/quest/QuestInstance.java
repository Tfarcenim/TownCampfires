package tfar.towncampfires.data.quest;

import com.google.common.collect.ImmutableList;
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

    private final ResourceLocation questID;
    private final UUID leader;
    List<UUID> members = new ArrayList<>();
    List<Integer> progress;

    List<Integer> failureProgress;

    Lazy<Quest> questLazy;

    Status status;

    public static QuestInstance begin(ResourceLocation questID, UUID leader,boolean instantStart) {
        return new QuestInstance(questID,leader,instantStart ? Status.IN_PROGRESS : Status.NOT_STARTED);
    }

    public QuestInstance(ResourceLocation questID, UUID leader,Status status) {
        this.questID = questID;
        this.leader = leader;
        this.status = status;
        progress = new ArrayList<>();
        failureProgress = new ArrayList<>();
        questLazy= Lazy.of(() -> TownCampfires.questLoader.getQuestMap().get(questID));
    }

    public void toPacket(FriendlyByteBuf buf) {
        buf.writeResourceLocation(questID);
        buf.writeEnum(status);
        buf.writeUUID(leader);
        buf.writeCollection(progress, FriendlyByteBuf::writeInt);
        buf.writeCollection(failureProgress, FriendlyByteBuf::writeInt);
    }

    public static QuestInstance fromPacket(FriendlyByteBuf buf) {
        ResourceLocation questID = buf.readResourceLocation();
        Status active = buf.readEnum(Status.class);
        UUID leader = buf.readUUID();
        List<Integer> integers = buf.readList(FriendlyByteBuf::readInt);
        List<Integer> failIntegers = buf.readList(FriendlyByteBuf::readInt);

        QuestInstance questInstance = new QuestInstance(questID,leader,active);
        questInstance.progress = integers;
        questInstance.failureProgress = failIntegers;
        return questInstance;
    }

    public Quest quest() {
        return questLazy.get();
    }

    public void removeMember(ServerPlayer player) {
        members.remove(player.getUUID());
    }

    public ResourceLocation questID() {
        return questID;
    }

    public List<Integer> progress() {
        return progress;
    }
    public List<Integer> failureProgress() {
        return failureProgress;
    }


    public void setStatus(Status status) {
        this.status = status;
    }

    public Status status() {
        return status;
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

    public List<UUID> getMembers() {
        return ImmutableList.copyOf(members);
    }

    public UUID leader() {
        return leader;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("quest",questID.toString());
        tag.putString("leader",leader.toString());
        tag.putString("status",status.name());
        ListTag listTag = new ListTag();
        for (UUID uuid : members) {
            listTag.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("members",listTag);

        tag.putIntArray("progress",progress);

        return tag;
    }

    public static QuestInstance load(CompoundTag tag) {
        ResourceLocation questID = new ResourceLocation(tag.getString("quest"));
        UUID leader = UUID.fromString(tag.getString("leader"));
        Status status = Status.valueOf(tag.getString("status"));
        QuestInstance questInstance = new QuestInstance(questID,leader,status);
        ListTag listTag = tag.getList("members", Tag.TAG_STRING);

        for (Tag tag1 : listTag) {
            questInstance.addPlayer(UUID.fromString(tag1.getAsString()));
        }

        int[] ints = tag.getIntArray("progress");
        if (ints.length > 0) {
            questInstance.progress = NonNullList.withSize(ints.length, 0);
            for (int i = 0 ; i<ints.length;i++) {
                questInstance.progress.set(i,ints[i]);
            }
        }

        return questInstance;
    }

    public <T extends AbstractCriterionTriggerInstance> boolean check(SimpleCriterionTrigger<T> trigger, ServerPlayer pPlayer, Predicate<T> pTestTrigger) {
        if (!shouldCheck(pPlayer) || !status.active) return false;

        boolean update = checkFailure(trigger,pPlayer,pTestTrigger);
        update |= checkCriterias(trigger,pPlayer,pTestTrigger);

        if (update) {
            updateStatus();
        }
        return update;
    }
// solo The quest variables rules follow the first player who accepted the quest except for Death and Respawn.
    //all  The quest variables rules follow All players who accepted the quest.
    boolean shouldCheck(ServerPlayer player) {
       return switch (quest().type()) {
            case solo,preparation_solo -> player.getUUID().equals(leader);
            case preparation_multiplayer -> hasPlayer(player);
        };
    }

    public <T extends AbstractCriterionTriggerInstance> boolean checkFailure(SimpleCriterionTrigger<T> trigger, ServerPlayer pPlayer, Predicate<T> pTestTrigger) {
        Quest quest = quest();
        boolean update = false;
        var criterias = quest.failureCriterias();
        int criteriaCount = criterias.size();
        for (int i = 0 ; i <criteriaCount;i++) {
            Pair<QuestCriteria<?>, Integer> pair = criterias.get(i);
            QuestCriteria<?> questCriteria = pair.getFirst();
            CriterionTriggerInstance o = questCriteria.triggerInstance();

            if (questCriteria.trigger() == trigger) {
                if (pTestTrigger.test((T) o)) {
                    if (failureProgress.isEmpty()) {
                        failureProgress = NonNullList.withSize(criteriaCount,0);
                    }
                    failureProgress.set(i, failureProgress.get(i) + 1);
                    update = true;
                }
            }
        }
        return update;
    }

    public <T extends AbstractCriterionTriggerInstance> boolean checkCriterias(SimpleCriterionTrigger<T> trigger, ServerPlayer pPlayer, Predicate<T> pTestTrigger) {
        Quest quest = quest();
        boolean update = false;
        var criterias = quest.criterias();
        int criteriaCount = criterias.size();
        for (int i = 0 ; i <criteriaCount;i++) {
            Pair<QuestCriteria<?>, Integer> pair = criterias.get(i);
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
        return update;
    }

    public void updateStatus() {

        List<Pair<QuestCriteria<?>, Integer>> failCriterias = quest().failureCriterias();
        for (int i = 0; i < failCriterias.size(); i++) {
            Pair<QuestCriteria<?>, Integer> criteria = failCriterias.get(i);
            int progress = progress().isEmpty() || progress().size() <= i ? 0 : progress().get(i);
            int required = criteria.getSecond();
            if (progress >= required) {
                status = Status.FAILED;
                return;
            }
        }

        boolean complete = true;
        List<Pair<QuestCriteria<?>, Integer>> criterias = quest().criterias();
        for (int i = 0; i < criterias.size(); i++) {
            Pair<QuestCriteria<?>, Integer> criteria = criterias.get(i);
            int progress = progress().isEmpty() || progress().size() <= i ? 0 : progress().get(i);
            int required = criteria.getSecond();
            if (progress < required) {
                complete = false;
                break;
            }
        }
        if (complete) {
            status = Status.COMPLETE;
        }
    }

    public void grantRewards(ServerPlayer claimingPlayer, TownCampfire townCampfire) {
        quest().rewards().grant(claimingPlayer,townCampfire,quest().levelUp());
        removeMember(claimingPlayer);
    }


    public enum Status {
        NOT_STARTED(false),
        IN_PROGRESS(true),
        FAILED(false),
        COMPLETE(false);

        public final boolean active;

        Status(boolean active) {
            this.active = active;
        }
    }
}
