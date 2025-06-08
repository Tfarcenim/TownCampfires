package tfar.towncampfires.data.quest;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private Map<ResourceLocation, Quest> questMap = ImmutableMap.of();

    public QuestLoader() {
        super(GSON, "quests");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        ImmutableMap.Builder<ResourceLocation, Quest> builder = ImmutableMap.builder();

        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();

            try {
                Quest quest = fromJson(resourcelocation, GsonHelper.convertToJsonObject(entry.getValue(), "top element"));
                if (quest == null) {
                    LOGGER.info("Skipping loading campfire effect {} as it returned null", resourcelocation);
                    continue;
                }
                builder.put(resourcelocation, quest);
            } catch (IllegalArgumentException | JsonParseException jsonparseexception) {
                LOGGER.error("Parsing error loading campfire effect {}", resourcelocation, jsonparseexception);
            }
        }

        this.questMap = builder.build();
        LOGGER.info("Loaded {} campfire effects", questMap.size());
    }

    public Map<ResourceLocation, Quest> getQuestMap() {
        return questMap;
    }

    public List<ResourceLocation> getEligibleQuests(Holder<Biome> biome) {

        List<ResourceLocation> list = new ArrayList<>(questMap.keySet());

        return list;
    }

    public ResourceLocation lookup(Quest quest) {
        return questMap.keySet().stream()
                .filter(resourceLocationCampfireEffectEntry -> questMap.get(resourceLocationCampfireEffectEntry) == quest).findFirst().orElse(null);
    }

    public static Quest fromJson(ResourceLocation id, JsonObject pJson) {
        if (pJson.size() == 0) {
            return null;
        }
        return Quest.CODEC.decode(JsonOps.INSTANCE, pJson).resultOrPartial(LOGGER::error).orElseThrow().getFirst();
    }

    public void setFromServer(Map<ResourceLocation, Quest> quests) {
        this.questMap = quests;
    }


}
