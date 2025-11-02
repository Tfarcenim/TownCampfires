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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.common.util.Lazy;
import tfar.towncampfires.CampfireLevelData;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.compat.GameStagesCompat;
import tfar.towncampfires.compat.LoadedMods;

import java.util.*;
import java.util.function.Predicate;

public class QuestInstance {

    private final ResourceLocation questID;
    private final UUID leader;
    List<UUID> members = new ArrayList<>();
    List<Integer> progress;

    List<Integer> failureProgress;

    Lazy<Quest> questLazy;

    Status status;

    long startTime;

    public static QuestInstance create(CampfireLevelData data,ResourceLocation questID, UUID leader, boolean instantStart) {
        QuestInstance questInstance = new QuestInstance(questID, leader, instantStart ? Status.IN_PROGRESS : Status.PREP);
        if (instantStart) {
            questInstance.begin(data);
        }
        return questInstance;
    }

    public void begin(CampfireLevelData data) {
        members.forEach(member -> data.addAttempt(member,questID));
        if (LoadedMods.gamestages.loaded) {
            GameStagesCompat.onPlayersAcceptQuest(data.level.getServer(),quest(),members);
        }
        startTime = data.level.getGameTime();
    }

    QuestInstance(ResourceLocation questID, UUID leader,Status status) {
        this.questID = questID;
        this.leader = leader;
        this.status = status;
        members.add(leader);
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
        buf.writeLong(startTime);
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

        questInstance.startTime = buf.readLong();
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
        return members.contains(player.getUUID());
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
            case solo, prep_solo -> player.getUUID().equals(leader);
            case prep_mp -> hasPlayer(player);
        };
    }

    public boolean timeUp(long currentTime) {
        if (quest().failureCriteria().timer() < 0 || !status().active) return false;
        return currentTime - startTime >= quest().failureCriteria().timer();
    }

    public <T extends AbstractCriterionTriggerInstance> boolean checkFailure(SimpleCriterionTrigger<T> trigger, ServerPlayer pPlayer, Predicate<T> pTestTrigger) {
        Quest quest = quest();
        boolean update = false;
        var criterias = quest.failureCriteria().custom();
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
        if (!status.active) return false;
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

        List<Pair<QuestCriteria<?>, Integer>> failCriterias = quest().failureCriteria().custom();
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

    public void punishMembers(CampfireLevelData data,MinecraftServer server) {
        Set<UUID> toRemove = new HashSet<>();
        for (UUID member : members) {
            ServerPlayer player = server.getPlayerList().getPlayer(member);
            if (player != null) {
                quest().punishments().punish(player, null);
                toRemove.add(member);
            }else {
                data.addDeferredPunishment(questID, member);
            }
        }
        toRemove.forEach(members::remove);
    }

    public boolean isEffectForbidden(MobEffect effect) {
        return quest().failureCriteria().forbiddenBuffs().contains(effect);
    }

    public boolean isStageForbidden(String stage) {
        return quest().failureCriteria().forbiddenStages().contains(stage);
    }


    public enum Status {
        PREP(false),
        IN_PROGRESS(true),
        FAILED(false),
        COMPLETE(false);

        public final boolean active;

        Status(boolean active) {
            this.active = active;
        }
    }
}
