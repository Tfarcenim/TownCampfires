package tfar.towncampfires.data.quest;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.PredicateManager;
import org.slf4j.Logger;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.utils.WeightedList;

import java.util.*;

public class QuestLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PredicateManager manager;
    private Map<ResourceLocation, Quest> questMap = ImmutableMap.of();

    public QuestLoader(PredicateManager manager) {
        super(GSON, "quests");
        this.manager = manager;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        ImmutableMap.Builder<ResourceLocation, Quest> builder = ImmutableMap.builder();

        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();

            try {
                Quest quest = fromJson(resourcelocation, GsonHelper.convertToJsonObject(entry.getValue(), "top element"));
                if (quest == null) {
                    LOGGER.info("Skipping loading quest {} as it returned null", resourcelocation);
                    continue;
                } else if (quest.weight() <= 0) {
                    LOGGER.info("Skipping loading quest {} as it has a weight of 0 or less", resourcelocation);
                    continue;
                }
                builder.put(resourcelocation, quest);
            } catch (IllegalArgumentException | JsonParseException jsonparseexception) {
                LOGGER.error("Parsing error loading quest {}", resourcelocation, jsonparseexception);
            }
        }

        this.questMap = builder.build();
        LOGGER.info("Loaded {} quests", questMap.size());
    }

    public Map<ResourceLocation, Quest> getQuestMap() {
        return questMap;
    }


    public List<ResourceLocation> getPossibleQuests(TownCampfire campfire, ServerLevel level,boolean levelUp) {
        BlockPos location = campfire.location();
        Holder<Biome> biome = level.getBiome(location);
        List<ResourceLocation> list = new ArrayList<>();

        for (Map.Entry<ResourceLocation,Quest> entry : questMap.entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            Quest quest = entry.getValue();
            if (levelUp != quest.levelUp()) continue;
            QuestAppearanceConditions questAppearanceConditions = quest.appearanceConditions();
            boolean correctBiome = biome.is(questAppearanceConditions.biomeWhitelist())^!questAppearanceConditions.isWhiteList();
            if (!correctBiome)continue;

            boolean hasNearby = false;
            for (Holder<Biome> biomeHolder : campfire.getNearbyBiomes()) {
                if (biomeHolder.is(questAppearanceConditions.nearbyBiomes())) {
                    hasNearby = true;
                    break;
                }
            }

            if (!hasNearby) continue;

            boolean inRange = questAppearanceConditions.levelRange().test(campfire.getLevel());
            if (!inRange) continue;

            double spawnDist = Math.sqrt(level.getSharedSpawnPos().distSqr(location));
            boolean spawnCheck = questAppearanceConditions.spawnDistance().test(spawnDist);
            if (!spawnCheck) continue;
            list.add(resourceLocation);
        }

        return list;
    }

    public List<ResourceLocation> getEligibleQuests(TownCampfire campfire, ServerLevel level) {
        int maxQuests = campfire.getMaxQuests();

        List<ResourceLocation> possibleQuests = getPossibleQuests(campfire, level,false);

        if (maxQuests >= possibleQuests.size()) {
            return possibleQuests;
        }

        WeightedList<WeightedList.Wrapper<ResourceLocation>> weightedList = WeightedList.create();
        for (ResourceLocation resourceLocation : possibleQuests) {
            weightedList.add(WeightedList.Wrapper.wrap(resourceLocation,questMap.get(resourceLocation).weight()));
        }

        List<ResourceLocation> picked = new ArrayList<>();

        int remaining = maxQuests;
        Random random = new Random();

        while (remaining>0) {
            remaining--;
            picked.add(weightedList.getRandom(random,false).data());
        }
        return picked;
    }

    public ResourceLocation addLevelQuest(TownCampfire campfire, ServerLevel level) {
        List<ResourceLocation> possibleQuests = getPossibleQuests(campfire, level,true);

        WeightedList<WeightedList.Wrapper<ResourceLocation>> weightedList = WeightedList.create();
        for (ResourceLocation resourceLocation : possibleQuests) {
            weightedList.add(WeightedList.Wrapper.wrap(resourceLocation,questMap.get(resourceLocation).weight()));
        }

        Random random = new Random();

        return weightedList.getRandom(random,false).data();
    }

    public ResourceLocation lookup(Quest quest) {
        return questMap.keySet().stream()
                .filter(e -> Objects.equals(questMap.get(e), quest)).findFirst().orElse(null);
    }

    public Quest fromJson(ResourceLocation id, JsonObject pJson) {
        if (pJson.size() == 0) {
            return null;
        }
        return Quest.read(pJson,new DeserializationContext(id, this.manager));
    }

    public void setFromServer(Map<ResourceLocation, Quest> quests) {
        this.questMap = quests;
    }


}
