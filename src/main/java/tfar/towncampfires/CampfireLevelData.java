package tfar.towncampfires;

import net.minecraft.Util;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import tfar.towncampfires.config.TownCampfireConfig;
import tfar.towncampfires.data.quest.Quest;
import tfar.towncampfires.data.quest.QuestInstance;
import tfar.towncampfires.data.quest.criteria.FailureCriteria;
import tfar.towncampfires.network.ForgePacketHandler;
import tfar.towncampfires.network.client.S2CQuestAttemptPacket;
import tfar.towncampfires.network.client.S2CQuestInstancePacket;
import tfar.towncampfires.network.client.S2CTeleportsPacket;

import java.util.*;
import java.util.function.Predicate;

public class CampfireLevelData extends SavedData {

    public final ServerLevel level;
    private List<TownCampfire> campfiresByIndex = new ArrayList<>();
    private transient Map<BlockPos, TownCampfire> campfiresByPos = new HashMap<>();

    List<QuestInstance> currentQuests = new ArrayList<>();

    Map<UUID, Set<ResourceLocation>> completedQuests = new HashMap<>();

    private Map<UUID, TownCampfire> lastVisited = new HashMap<>();

    private Map<ResourceLocation, Set<UUID>> deferredPunishments = new HashMap<>();


    private Map<ResourceLocation, Set<UUID>> deferredRewards = new HashMap<>();

    private Map<UUID, Map<ResourceLocation, Integer>> totalAttempts = new HashMap<>();

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

    public void sendTeleportInfo(ServerPlayer player) {
        List<TownCampfire> campfires = new ArrayList<>();
        campfiresByIndex.forEach(campfire -> campfires.add(campfire));
        ForgePacketHandler.sendToClient(new S2CTeleportsPacket(campfires), player);

    }

    public void addDeferredPunishment(ResourceLocation questID, UUID uuid) {
        Set<UUID> uuids = deferredPunishments.computeIfAbsent(questID, resourceLocation -> new HashSet<>());
        uuids.add(uuid);
    }

    public void sendDataTo(ServerPlayer player) {
        List<QuestInstance> questInstances = new ArrayList<>();
        for (QuestInstance questInstance : currentQuests) {
            if (questInstance.hasPlayer(player) || questInstance.status() == QuestInstance.Status.PREP) {//if the player is a member OR if quest can be joined
                questInstances.add(questInstance);
            }
        }

        var map = totalAttempts.getOrDefault(player.getUUID(), Map.of());
        ForgePacketHandler.sendToClient(new S2CQuestAttemptPacket(map), player);
        ForgePacketHandler.sendToClient(new S2CQuestInstancePacket(questInstances), player);
    }

    public void checkDeathCriteria(ServerPlayer player) {
        List<QuestInstance> toUpdate = new ArrayList<>();
        for (QuestInstance questInstance : currentQuests) {
            if (questInstance.status().active && questInstance.hasPlayer(player)) {
                FailureCriteria failureCriteria = questInstance.quest().failureCriteria();
                if (failureCriteria.death()) {
                    questInstance.setStatus(QuestInstance.Status.FAILED);
                    questInstance.punishMembers(this, player.server);
                    toUpdate.add(questInstance);
                }
            }
        }


        for (QuestInstance questInstance : toUpdate) {
            markQuestCompleted(player, questInstance.questID());
            if (questInstance.getMembers().isEmpty()) {
                currentQuests.remove(questInstance);
            }
        }

        for (ServerPlayer play : player.server.getPlayerList().getPlayers()) {
            sendDataTo(play);
        }
    }

    public void checkEffectAddedCriteria(ServerPlayer player, MobEffect effect) {
        List<QuestInstance> toUpdate = new ArrayList<>();
        for (QuestInstance questInstance : currentQuests) {
            if (questInstance.status().active && questInstance.hasPlayer(player)) {
                if (questInstance.isEffectForbidden(effect)) {
                    questInstance.setStatus(QuestInstance.Status.FAILED);
                    questInstance.punishMembers(this, level.getServer());
                    toUpdate.add(questInstance);
                }
            }
        }

        for (QuestInstance questInstance : toUpdate) {
            List<UUID> members = questInstance.getMembers();
            for (UUID uuid : members) {
                markQuestCompleted(uuid, questInstance.questID());
            }
            if (questInstance.getMembers().isEmpty()) {
                currentQuests.remove(questInstance);
            }
        }
        if (!toUpdate.isEmpty()) {
            updatePlayers = true;
            setDirty();
        }
    }

    public void checkStageAddedCriteria(ServerPlayer player, String stage) {
        List<QuestInstance> toUpdate = new ArrayList<>();
        for (QuestInstance questInstance : currentQuests) {
            if (questInstance.status().active && questInstance.hasPlayer(player)) {
                if (questInstance.isStageForbidden(stage)) {
                    questInstance.setStatus(QuestInstance.Status.FAILED);
                    questInstance.punishMembers(this, level.getServer());
                    toUpdate.add(questInstance);
                }
            }
        }

        for (QuestInstance questInstance : toUpdate) {
            List<UUID> members = questInstance.getMembers();
            for (UUID uuid : members) {
                markQuestCompleted(uuid, questInstance.questID());
            }
            if (questInstance.getMembers().isEmpty()) {
                currentQuests.remove(questInstance);
            }
        }
        if (!toUpdate.isEmpty()) {
            updatePlayers = true;
            setDirty();
        }
    }

    public void checkTimeCriteria() {
        List<QuestInstance> toUpdate = new ArrayList<>();
        for (QuestInstance questInstance : currentQuests) {
            if (questInstance.status().active) {
                if (questInstance.timeUp(level.getGameTime())) {
                    questInstance.setStatus(QuestInstance.Status.FAILED);
                    questInstance.punishMembers(this, level.getServer());
                    toUpdate.add(questInstance);
                }
            }
        }

        for (QuestInstance questInstance : toUpdate) {
            List<UUID> members = questInstance.getMembers();
            for (UUID uuid : members) {
                markQuestCompleted(uuid, questInstance.questID());
            }
            if (questInstance.getMembers().isEmpty()) {
                currentQuests.remove(questInstance);
            }
        }
        if (!toUpdate.isEmpty()) {
            updatePlayers = true;
            setDirty();
        }
    }

    public <T extends AbstractCriterionTriggerInstance> void
    checkQuests(SimpleCriterionTrigger<T> trigger, ServerPlayer pPlayer, Predicate<T> pTestTrigger) {
        List<QuestInstance> toUpdate = new ArrayList<>();
        List<ServerPlayer> needUpdates = new ArrayList<>();
        for (QuestInstance questInstance : currentQuests) {
            if (questInstance.status().active) {
                boolean check = questInstance.check(trigger, pPlayer, pTestTrigger);
                if (check) {
                    needUpdates.add(pPlayer);
                    if (questInstance.status() == QuestInstance.Status.FAILED) {
                        toUpdate.add(questInstance);
                        MinecraftServer server = pPlayer.server;
                        questInstance.punishMembers(this, server);
                    }
                }
            }
        }
        for (ServerPlayer player : needUpdates) {
            for (QuestInstance questInstance : toUpdate) {
                markQuestCompleted(player, questInstance.questID());
                if (questInstance.getMembers().isEmpty()) {
                    currentQuests.remove(questInstance);
                }
            }
            sendDataTo(player);
        }
        setDirty();
    }

    public int getUsedSlots(ServerPlayer player) {
        int slotsUsed = 0;
        for (QuestInstance questInstance : currentQuests) {
            if (questInstance.hasPlayer(player)) ;
            slotsUsed += questInstance.quest().slots();
        }
        return slotsUsed;
    }

    public int getFreeSlots(ServerPlayer player) {
        return TownCampfireConfig.CONFIG.slots_per_player.get() - getUsedSlots(player);
    }

    public boolean hasEnoughSlots(ServerPlayer player, Quest quest) {
        return quest.slots() + getUsedSlots(player) <= TownCampfireConfig.CONFIG.slots_per_player.get();
    }

    public void addAttempt(ServerPlayer player, ResourceLocation questID) {
        addAttempt(player.getUUID(), questID);
    }

    public void syncAttempts(UUID player) {
        ServerPlayer player1 = level.getServer().getPlayerList().getPlayer(player);
        if (player1 != null) {
            ForgePacketHandler.sendToClient(new S2CQuestAttemptPacket(totalAttempts.getOrDefault(player, Map.of())), player1);
        }
    }

    public void addAttempt(UUID player, ResourceLocation questID) {
        Map<ResourceLocation, Integer> attempt = totalAttempts.computeIfAbsent(player, uuid -> new HashMap<>());
        attempt.put(questID, attempt.getOrDefault(questID, 0) + 1);
        syncAttempts(player);
    }


    public void startOrPrepQuest(ServerPlayer player, ResourceLocation questID, TownCampfire campfire) {

        if (!campfire.getQuestIds().contains(questID)) {
            TownCampfires.LOGGER.warn("{} Attempted to start nonexistent quest {}", player, questID);
        } else {
            Quest quest = TownCampfires.questLoader.getQuestMap().get(questID);
            QuestInstance existing = findExistingQuest(quest, player);
            if (existing == null) {//make a new quest instance
                if (!hasEnoughSlots(player, quest)) {
                    TownCampfires.LOGGER.warn("{} Attempted to start quest {} without enough slots", player, questID);
                }
                switch (quest.type()) {
                    case solo -> {
                        QuestInstance questInstance = QuestInstance.create(this, questID, player.getUUID(), true);
                        currentQuests.add(questInstance);
                        updatePlayers = true;
                        setDirty();
                    }
                    case prep_solo, prep_mp -> {
                        //check for other existing instances first!
                        boolean wasAdded = false;
                        for (QuestInstance questInstance : this.currentQuests) {
                            Quest quest1 = questInstance.quest();
                            if (quest1 == quest && !questInstance.hasPlayer(player)) {
                                questInstance.addPlayer(player);
                                wasAdded = true;
                                updatePlayers = true;
                                setDirty();
                                break;
                            }
                        }

                        if (!wasAdded) {
                            QuestInstance questInstance = QuestInstance.create(this, questID, player.getUUID(), false);
                            currentQuests.add(questInstance);
                            updatePlayers = true;
                            setDirty();
                        }
                    }
                }
            } else {
                switch (quest.type()) {
                    case solo -> {

                    }
                    case prep_solo, prep_mp -> {
                        existing.setStatus(QuestInstance.Status.IN_PROGRESS);
                        existing.begin(this);
                        updatePlayers = true;
                        setDirty();
                    }
                }
            }
        }
    }

    boolean updatePlayers;

    @Override
    public void setDirty() {
        super.setDirty();
        if (updatePlayers) {
            MinecraftServer server = level.getServer();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sendDataTo(player);
            }
            updatePlayers = false;
        }
    }

    public void finishQuest(ServerPlayer player, ResourceLocation questID, TownCampfire townCampfire) {
        Quest quest = TownCampfires.questLoader.getQuestMap().get(questID);
        QuestInstance existing = findExistingQuest(quest, player);
        if (existing == null) {//make a new quest instance
            TownCampfires.LOGGER.warn("{} Attempted to claim reward for nonexistent quest", player);
        } else {
            if (existing.status() == QuestInstance.Status.COMPLETE && existing.hasPlayer(player)) {
                existing.grantRewards(player, townCampfire);
                if (existing.getMembers().isEmpty()) {
                    currentQuests.remove(existing);
                }

                markQuestCompleted(player, questID);
                sendDataTo(player);
                setDirty();
            }
        }
    }

    public void markQuestCompleted(ServerPlayer player, ResourceLocation questID) {
        Set<ResourceLocation> set = completedQuests.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
        set.add(questID);
    }

    public void markQuestCompleted(UUID uuid, ResourceLocation questID) {
        Set<ResourceLocation> set = completedQuests.computeIfAbsent(uuid, k -> new HashSet<>());
        set.add(questID);
    }

    @Nullable
    public QuestInstance findExistingQuest(Quest quest, ServerPlayer player) {
        for (QuestInstance questInstance : this.currentQuests) {
            Quest quest1 = questInstance.quest();
            if (quest1 == quest && questInstance.isLeader(player)) {
                return questInstance;
            }
        }
        return null;
    }

    public static CampfireLevelData getOrCreate(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(compoundTag -> loadStatic(compoundTag, level), () -> new CampfireLevelData(level), TownCampfires.MODID);
    }

    @Nullable
    public static CampfireLevelData get(ServerLevel level) {
        return level.getDataStorage()
                .get(compoundTag -> loadStatic(compoundTag, level), TownCampfires.MODID);
    }

    static CampfireLevelData loadStatic(CompoundTag compoundTag, ServerLevel level) {
        CampfireLevelData data = new CampfireLevelData(level);
        data.load(compoundTag);
        return data;
    }

    protected void load(CompoundTag compoundTag) {
        if (compoundTag.contains("campfires")) {
            ListTag campfiresTag = compoundTag.getList("campfires", Tag.TAG_COMPOUND);

            for (Tag tag : campfiresTag) {
                TownCampfire townCampfire = TownCampfire.load((CompoundTag) tag, this);
                campfiresByIndex.add(townCampfire);
                campfiresByPos.put(townCampfire.location(), townCampfire);
            }
        }
        if (compoundTag.contains("last_visited")) {
            CompoundTag tag = compoundTag.getCompound("last_visited");
            for (String key : tag.getAllKeys()) {
                int[] ints = tag.getIntArray(key);
                lastVisited.put(UUID.fromString(key), campfiresByPos.get(new BlockPos(ints[0], ints[1], ints[2])));
            }
        }

        ListTag currentQuestsTag = compoundTag.getList("current_quests", CompoundTag.TAG_COMPOUND);
        for (Tag t : currentQuestsTag) {
            QuestInstance questInstance = QuestInstance.load((CompoundTag) t);
            if (questInstance != null) {
                currentQuests.add(questInstance);
            }
        }


        CompoundTag completedQuestTag = compoundTag.getCompound("completed_quests");
        for (String key : completedQuestTag.getAllKeys()) {
            ListTag listTag = completedQuestTag.getList(key, Tag.TAG_STRING);
            Set<ResourceLocation> set = new HashSet<>();
            for (Tag t : listTag) {
                StringTag stringTag = (StringTag) t;
                ResourceLocation location = new ResourceLocation(stringTag.getAsString());
                if (TownCampfires.questLoader.questStillExists(location)) {
                    set.add(location);
                }
            }
            completedQuests.put(UUID.fromString(key), set);
        }


        loadAttemptsTag(compoundTag);

    }

    void loadAttemptsTag(CompoundTag tag) {
        CompoundTag totalAttemptsTag = tag.getCompound("total_attempts");
        for (String key : totalAttemptsTag.getAllKeys()) {
            UUID uuid = UUID.fromString(key);
            CompoundTag rlTag = totalAttemptsTag.getCompound(key);
            Map<ResourceLocation, Integer> map = new HashMap<>();
            for (String t : rlTag.getAllKeys()) {
                int i = rlTag.getInt(t);
                map.put(new ResourceLocation(t), i);
            }
            totalAttempts.put(uuid, map);
        }
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag) {
        ListTag campfiresByIndexTag = new ListTag();

        for (TownCampfire campfire : campfiresByIndex) {
            campfiresByIndexTag.add(campfire.save());
        }
        pCompoundTag.put("campfires", campfiresByIndexTag);


        CompoundTag tag = new CompoundTag();
        for (Map.Entry<UUID, TownCampfire> entry : lastVisited.entrySet()) {
            BlockPos pos = entry.getValue().location();
            tag.putIntArray(entry.getKey().toString(), new int[]{pos.getX(), pos.getY(), pos.getZ()});
        }
        pCompoundTag.put("last_visited", tag);


        ListTag currentQuests = new ListTag();
        for (QuestInstance questInstance : this.currentQuests) {
            currentQuests.add(questInstance.save());
        }
        pCompoundTag.put("current_quests", currentQuests);

        CompoundTag completedQuestTag = new CompoundTag();

        for (Map.Entry<UUID, Set<ResourceLocation>> entry : completedQuests.entrySet()) {
            Set<ResourceLocation> resourceLocations = entry.getValue();
            ListTag listTag1 = new ListTag();
            for (ResourceLocation resourceLocation : resourceLocations) {
                listTag1.add(StringTag.valueOf(resourceLocation.toString()));
            }
            completedQuestTag.put(entry.getKey().toString(), listTag1);
        }

        pCompoundTag.put("completed_quests", completedQuestTag);

        CompoundTag deferredPunishmentsTag = new CompoundTag();

        for (Map.Entry<ResourceLocation, Set<UUID>> entry : deferredPunishments.entrySet()) {
            Set<UUID> uuids = entry.getValue();
            ListTag listTag1 = new ListTag();
            for (UUID uuid : uuids) {
                listTag1.add(StringTag.valueOf(uuid.toString()));
            }
            deferredPunishmentsTag.put(entry.getKey().toString(), listTag1);
        }

        saveAttemptsTag(pCompoundTag);

        return pCompoundTag;
    }

    void saveAttemptsTag(CompoundTag tag) {
        CompoundTag attemptsTag = new CompoundTag();

        for (Map.Entry<UUID, Map<ResourceLocation, Integer>> entry : totalAttempts.entrySet()) {
            Map<ResourceLocation, Integer> locationIntegerMap = entry.getValue();

            if (locationIntegerMap != null) {
                CompoundTag rlTag = new CompoundTag();

                for (Map.Entry<ResourceLocation, Integer> entry1 : locationIntegerMap.entrySet()) {
                    rlTag.put(entry1.getKey().toString(), IntTag.valueOf(entry1.getValue()));
                }
                attemptsTag.put(entry.getKey().toString(), rlTag);
            }
        }

        tag.put("total_attempts", attemptsTag);

    }

    public void tick() {
        boolean shouldRefresh = level.getGameTime() % TownCampfireConfig.CONFIG.refresh_timer.get() == 0;
        if (shouldRefresh) {
            refresh();
        }
        checkTimeCriteria();
    }

    public void refresh() {
        completedQuests.clear();
        totalAttempts.clear();
        List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers();
        players.forEach(player -> syncAttempts(player.getUUID()));
        setDirty();

        for (TownCampfire campfire : campfiresByIndex) {
            campfire.update(level, true);
        }
    }

    public void reset() {
        currentQuests.clear();
        totalAttempts.clear();
        completedQuests.clear();
        level.getServer().getPlayerList().getPlayers().forEach(this::sendDataTo);
        setDirty();
    }

    public void tryDeliver(ServerPlayer player, ResourceLocation questID, TownCampfire townCampfire) {
        Quest quest = TownCampfires.questLoader.getQuestMap().get(questID);
        if (quest != null) {
            QuestInstance questInstance = findExistingQuest(quest,player);
            if (questInstance != null) {
                if (questInstance.tryDeliver(player)){
                    updatePlayers = true;
                    setDirty();
                }
            }
        }
    }
}

