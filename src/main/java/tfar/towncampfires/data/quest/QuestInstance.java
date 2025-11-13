package tfar.towncampfires.data.quest;

import com.google.common.collect.ImmutableList;
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
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import tfar.towncampfires.CampfireLevelData;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.compat.GameStagesCompat;
import tfar.towncampfires.compat.LoadedMods;
import tfar.towncampfires.data.quest.criteria.CriteriaType;
import tfar.towncampfires.data.quest.criteria.Delivery;

import java.util.*;
import java.util.function.Predicate;

public class QuestInstance {

    private final ResourceLocation questID;
    private final UUID leader;
    List<UUID> members = new ArrayList<>();
    Map<CriteriaType,List<Integer>> progress = new EnumMap<>(CriteriaType.class);

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
        questLazy= Lazy.of(() -> TownCampfires.questLoader.getQuestMap().get(questID));

    }

    public void toPacket(FriendlyByteBuf buf) {
        buf.writeResourceLocation(questID);
        buf.writeEnum(status);
        buf.writeUUID(leader);
        buf.writeMap(progress, FriendlyByteBuf::writeEnum,(buf1, integers) -> buf1.writeCollection(integers, FriendlyByteBuf::writeInt));
        buf.writeLong(startTime);
    }

    public static QuestInstance fromPacket(FriendlyByteBuf buf) {
        ResourceLocation questID = buf.readResourceLocation();
        Status active = buf.readEnum(Status.class);
        UUID leader = buf.readUUID();
        Map<CriteriaType,List<Integer>> map = buf.readMap(buf1 -> buf1.readEnum(CriteriaType.class), buf1 -> buf1.readList(FriendlyByteBuf::readInt));

        QuestInstance questInstance = new QuestInstance(questID,leader,active);
        questInstance.progress = map;
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

    public List<Integer> customProgress() {
        return progress.computeIfAbsent(CriteriaType.CUSTOM,criteriaType -> new ArrayList<>());
    }

    public List<Integer> deliveryProgress() {
        return progress.computeIfAbsent(CriteriaType.DELIVERY,criteriaType -> NonNullList.withSize(quest().successCriteria().deliveries().size(),0));
    }

    public List<Integer> failureProgress() {
        return progress.computeIfAbsent(CriteriaType.FAILURE,criteriaType -> new ArrayList<>());
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

        CompoundTag progressTag = new CompoundTag();
        for (Map.Entry<CriteriaType,List<Integer>> entry : progress.entrySet()) {
            progressTag.putIntArray(entry.getKey().name(), entry.getValue());
        }
        tag.put("progress",progressTag);
        return tag;
    }

    @Nullable
    public static QuestInstance load(CompoundTag tag) {
        ResourceLocation questID = new ResourceLocation(tag.getString("quest"));

        if (!TownCampfires.questLoader.questStillExists(questID)) return null;

        UUID leader = UUID.fromString(tag.getString("leader"));
        Status status = Status.valueOf(tag.getString("status"));
        QuestInstance questInstance = new QuestInstance(questID,leader,status);
        ListTag listTag = tag.getList("members", Tag.TAG_STRING);

        for (Tag tag1 : listTag) {
            questInstance.addPlayer(UUID.fromString(tag1.getAsString()));
        }

        CompoundTag progressTag = tag.getCompound("progress");


        for(String key : progressTag.getAllKeys()) {
            int[] ints = progressTag.getIntArray(key);
            for (int i = 0 ; i<ints.length;i++) {
                questInstance.progress.put(CriteriaType.valueOf(key),convert(ints));
            }
        }

        return questInstance;
    }

    static List<Integer> convert(int[] ints) {
        return new ArrayList<>(Arrays.stream(ints).boxed().toList());
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
            QuestCriteria<?> pair = criterias.get(i);
            CriterionTriggerInstance o = pair.triggerInstance();

            if (pair.trigger() == trigger) {
                if (pTestTrigger.test((T) o)) {
                    List<Integer> failureProgress = progress.get(CriteriaType.FAILURE);
                    if (failureProgress().isEmpty()) {
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
        var criterias = quest.successCriteria().custom();
        int criteriaCount = criterias.size();
        for (int i = 0 ; i <criteriaCount;i++) {
            QuestCriteria<?> questCriteria = criterias.get(i);
            CriterionTriggerInstance o = questCriteria.triggerInstance();

            if (questCriteria.trigger() == trigger) {
                if (pTestTrigger.test((T) o)) {
                    List<Integer> customProgress = progress.get(CriteriaType.CUSTOM);
                    if (customProgress.isEmpty()) {
                        customProgress = NonNullList.withSize(criteriaCount,0);
                    }
                    customProgress.set(i, customProgress.get(i) + 1);
                    update = true;
                }
            }
        }
        return update;
    }

    public void updateStatus() {

        List<QuestCriteria<?>> failCriterias = quest().failureCriteria().custom();
        for (int i = 0; i < failCriterias.size(); i++) {
            QuestCriteria<?> criteria = failCriterias.get(i);
            int progress = customProgress().isEmpty() || customProgress().size() <= i ? 0 : customProgress().get(i);
            int required = criteria.count();
            if (progress >= required) {
                status = Status.FAILED;
                return;
            }
        }

        boolean complete = true;
        List<QuestCriteria<?>> criterias = quest().successCriteria().custom();
        for (int i = 0; i < criterias.size(); i++) {
            QuestCriteria<?> criteria = criterias.get(i);
            int progress = customProgress().isEmpty() || customProgress().size() <= i ? 0 : customProgress().get(i);
            int required = criteria.count();
            if (progress < required) {
                complete = false;
                break;
            }
        }


        List<Delivery> deliveries = quest().successCriteria().deliveries();
        for (int i = 0; i < deliveries.size(); i++) {
            Delivery criteria = deliveries.get(i);
            List<Integer> deliveryProgress = deliveryProgress();
            int progress = deliveryProgress.isEmpty() || deliveryProgress.size() <= i ? 0 : deliveryProgress.get(i);
            int required = criteria.required();
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

    public boolean tryDeliver(ServerPlayer player) {
        if (status == QuestInstance.Status.IN_PROGRESS && hasPlayer(player)) {
            boolean didAnything = false;
            List<Delivery> deliveries = quest().successCriteria().deliveries();
            for (int i = 0; i < deliveries.size(); i++) {
                Delivery delivery = deliveries.get(i);
                if (delivery.required() > deliveryProgress().get(i)) {
                    IItemHandler handler = player.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
                    if (handler != null) {
                        for (int slot = 0; slot < handler.getSlots();slot++ ) {
                            ItemStack stack = handler.getStackInSlot(slot);
                            if (delivery.ingredient().test(stack)) {
                                int extractAmount = delivery.required() - deliveryProgress().get(i);
                                ItemStack extracted = handler.extractItem(slot,extractAmount,false);
                                if (!extracted.isEmpty()) {
                                    deliveryProgress().set(i,deliveryProgress().get(i)+extracted.getCount());
                                    didAnything = true;
                                }
                            }
                        }
                    }
                }
            }
            if (didAnything) {
                updateStatus();
            }
            return didAnything;
        }
        return false;
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
