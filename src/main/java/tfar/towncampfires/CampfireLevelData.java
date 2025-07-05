package tfar.towncampfires;

import net.minecraft.Util;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import tfar.towncampfires.config.TownCampfireConfig;
import tfar.towncampfires.data.quest.Quest;
import tfar.towncampfires.data.quest.QuestInstance;
import tfar.towncampfires.data.quest.QuestRewards;
import tfar.towncampfires.network.ForgePacketHandler;
import tfar.towncampfires.network.client.S2CQuestInstancePacket;

import java.util.*;
import java.util.function.Predicate;

public class CampfireLevelData extends SavedData {

    private final ServerLevel level;
    private List<TownCampfire> campfiresByIndex = new ArrayList<>();
    private transient Map<BlockPos, TownCampfire> campfiresByPos = new HashMap<>();

    List<QuestInstance> currentQuests = new ArrayList<>();

    Map<UUID,Set<ResourceLocation>> completedQuests = new HashMap<>();

    private Map<UUID, TownCampfire> lastVisited = new HashMap<>();

    public CampfireLevelData(ServerLevel pLevel) {
        this.level = pLevel;
    }

    public List<TownCampfire> getCampfiresByIndex() {
        return campfiresByIndex;
    }

    @Nullable
    public TownCampfire byLocation(BlockPos location) {
        return campfiresByPos.get(location);
    }

    public TownCampfire byIndex(int index) {
        return campfiresByIndex.get(index);
    }

    public void save(TownCampfireBlockEntity be) {
        BlockPos blockPos = be.getBlockPos();
        if (!campfiresByPos.containsKey(blockPos)) {
            TownCampfire townCampfire = new TownCampfire(blockPos,
                    Util.getRandom(TownCampfireConfig.CONFIG.defaultNames.get(), be.getLevel().random),
                    0, 0, 0, new ArrayList<>(), TownCampfireConfig.CONFIG.starting_quests
                    .get().pick(be.getLevel().random));

            townCampfire.refresh(level);

            be.setLinkedCampfire(townCampfire);

            campfiresByIndex.add(townCampfire);
            campfiresByPos.put(blockPos, townCampfire);
            setDirty();
        } else {
            be.setLinkedCampfire(campfiresByPos.get(be.getBlockPos()));
        }
    }

    public void markVisit(ServerPlayer player, TownCampfire campfire) {
        lastVisited.put(player.getUUID(), campfire);
        setDirty();
    }

    public void sendQuestsTo(ServerPlayer player) {
        List<QuestInstance> questInstances = new ArrayList<>();
        for (QuestInstance questInstance : currentQuests) {
            if (questInstance.hasPlayer(player) || !questInstance.isActive()) {//if the player is a member OR if quest can be joined
                questInstances.add(questInstance);
            }
        }
        ForgePacketHandler.sendToClient(new S2CQuestInstancePacket(questInstances), player);
    }

    public <T extends AbstractCriterionTriggerInstance> void
    checkQuests(SimpleCriterionTrigger<T> trigger, ServerPlayer pPlayer, Predicate<T> pTestTrigger) {
        for (QuestInstance questInstance : currentQuests) {
            if (questInstance.isActive()) {
                boolean check = questInstance.check(trigger, pPlayer, pTestTrigger);
                if (check) {
                    setDirty();
                    sendQuestsTo(pPlayer);
                }
            }
        }
    }

    public void startQuest(ServerPlayer player, ResourceLocation questID, TownCampfire campfire) {

        if (!campfire.getQuestIds().contains(questID)) {
            TownCampfires.LOGGER.warn("{} Attempted to start nonexistent quest {}", player, questID);
        } else {
            Quest quest = TownCampfires.questLoader.getQuestMap().get(questID);
            QuestInstance existing = findExistingQuest(quest, player);
            if (existing == null) {//make a new quest instance
                switch (quest.type()) {
                    case normal, level -> {
                        QuestInstance questInstance = new QuestInstance(questID, player.getUUID());
                        currentQuests.add(questInstance);
                        questInstance.setActive(true);
                        sendQuestsTo(player);
                        setDirty();
                    }
                    case preparation_solo, preparation_all -> {
                        //check for other existing instances first!
                        boolean wasAdded = false;
                        for (QuestInstance questInstance : this.currentQuests) {
                            Quest quest1 = questInstance.quest();
                            if (quest1 == quest && !questInstance.hasPlayer(player)) {
                                questInstance.addPlayer(player);
                                wasAdded = true;
                                setDirty();
                                break;
                            }
                        }

                        if (!wasAdded) {
                            QuestInstance questInstance = new QuestInstance(questID, player.getUUID());
                            currentQuests.add(questInstance);
                            setDirty();
                        }
                    }
                }
            } else {
                switch (quest.type()) {
                    case normal -> {

                    }
                    case preparation_solo -> {
                        existing.setActive(true);
                    }
                    case preparation_all -> {
                        existing.setActive(true);
                    }
                    case level -> {
                    }
                }
            }
        }
    }

    public void finishQuest(ServerPlayer player, ResourceLocation questID, TownCampfire townCampfire) {
        Quest quest = TownCampfires.questLoader.getQuestMap().get(questID);
        QuestInstance existing = findExistingQuest(quest, player);
        if (existing == null) {//make a new quest instance
            TownCampfires.LOGGER.warn("{} Attempted to claim reward for nonexistent quest",player);
        } else {
            if (existing.isActive() && existing.isFinished() && existing.hasPlayer(player)) {
                QuestRewards questRewards = existing.quest().rewards();
                questRewards.grant(player,townCampfire,quest.type() == Quest.Type.level);
                existing.removeMember(player);
                if (existing.getMembers().isEmpty()) {
                    currentQuests.remove(existing);
                }

                Set<ResourceLocation> set = completedQuests.computeIfAbsent(player.getUUID(), k -> new HashSet<>());

                set.add(questID);

                sendQuestsTo(player);
                setDirty();
            }
        }
    }

    @javax.annotation.Nullable
    public QuestInstance findExistingQuest(Quest quest, ServerPlayer player) {
        for (QuestInstance questInstance : this.currentQuests) {
            Quest quest1 = questInstance.quest();
            if (quest1 == quest && questInstance.isLeader(player)) {
                return questInstance;
            }
        }
        return null;
    }

    public TownCampfire getLastVisited(UUID uuid) {
        return lastVisited.get(uuid);
    }

    public static CampfireLevelData getOrCreate(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(compoundTag -> loadStatic(compoundTag,level), () -> new CampfireLevelData(level),TownCampfires.MODID);
    }

    static CampfireLevelData loadStatic(CompoundTag compoundTag, ServerLevel level) {
        CampfireLevelData data = new CampfireLevelData(level);
        data.load(compoundTag);
        return data;
    }

    protected void load(CompoundTag compoundTag) {
        if (compoundTag.contains("campfires")) {
            ListTag campfiresTag = compoundTag.getList("campfires",Tag.TAG_COMPOUND);

            for (Tag tag : campfiresTag) {
                TownCampfire townCampfire = TownCampfire.load((CompoundTag) tag,this);
                campfiresByIndex.add(townCampfire);
                campfiresByPos.put(townCampfire.location(), townCampfire);
            }
        }
        if (compoundTag.contains("last_visited")) {
            CompoundTag tag = compoundTag.getCompound("last_visited");
            for (String key : tag.getAllKeys()) {
                int[] ints = tag.getIntArray(key);
                lastVisited.put(UUID.fromString(key),campfiresByPos.get(new BlockPos(ints[0],ints[1],ints[2])));
            }
        }

        ListTag currentQuestsTag = compoundTag.getList("current_quests", CompoundTag.TAG_COMPOUND);
        for (Tag t : currentQuestsTag) {
            QuestInstance questInstance = QuestInstance.load((CompoundTag) t);
            currentQuests.add(questInstance);
        }


        CompoundTag completedQuestTag = compoundTag.getCompound("completed_quests");
        for (String key : completedQuestTag.getAllKeys()) {
            ListTag listTag = completedQuestTag.getList(key,Tag.TAG_STRING);
            Set<ResourceLocation> set = new HashSet<>();
            for (Tag t : listTag) {
                StringTag stringTag = (StringTag) t;
                set.add(new ResourceLocation(stringTag.getAsString()));
            }
            completedQuests.put(UUID.fromString(key),set);
        }
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag) {
        ListTag listTag = new ListTag();

        for (TownCampfire campfire : campfiresByIndex) {
            listTag.add(campfire.save());
        }
        pCompoundTag.put("campfires",listTag);


        CompoundTag tag = new CompoundTag();
        for (Map.Entry<UUID,TownCampfire> entry: lastVisited.entrySet()) {
            BlockPos pos = entry.getValue().location();
            tag.putIntArray(entry.getKey().toString(),new int[]{pos.getX(),pos.getY(),pos.getZ()});
        }
        pCompoundTag.put("last_visited",tag);


        ListTag currentQuests = new ListTag();
        for (QuestInstance questInstance : this.currentQuests) {
            currentQuests.add(questInstance.save());
        }
        pCompoundTag.put("current_quests",currentQuests);

        CompoundTag completedQuestTag = new CompoundTag();

        for (Map.Entry<UUID,Set<ResourceLocation>> entry : completedQuests.entrySet()) {
            Set<ResourceLocation> resourceLocations = entry.getValue();
            ListTag listTag1 = new ListTag();
            for (ResourceLocation resourceLocation : resourceLocations) {
                listTag1.add(StringTag.valueOf(resourceLocation.toString()));
            }
            completedQuestTag.put(entry.getKey().toString(),listTag1);
        }

        pCompoundTag.put("completed_quests",completedQuestTag);

        return pCompoundTag;
    }

    public void tick() {
        boolean shouldRefresh = level.getGameTime() % TownCampfireConfig.CONFIG.refresh_timer.get() == 0;

        if (shouldRefresh) {
            completedQuests.clear();
            setDirty();
        }

        for (TownCampfire campfire : campfiresByIndex) {
            campfire.update(level,shouldRefresh);
        }
    }
}
