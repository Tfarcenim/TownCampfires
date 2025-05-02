package tfar.towncampfires.data;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import tfar.towncampfires.CampfireEffect;

import java.util.Map;

public class CampfireEffectReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private Map<ResourceLocation, CampfireEffect> campfireEffects = ImmutableMap.of();
    public CampfireEffectReloadListener() {
        super(GSON,"campfire_effects");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        ImmutableMap.Builder<ResourceLocation, CampfireEffect> builder = ImmutableMap.builder();

        for(Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();

            try {
                CampfireEffect campfireEffect = fromJson(resourcelocation, GsonHelper.convertToJsonObject(entry.getValue(), "top element"));
                if (campfireEffect == null) {
                    LOGGER.info("Skipping loading campfire effect {} as it returned null", resourcelocation);
                    continue;
                }
                builder.put(resourcelocation, campfireEffect);
            } catch (IllegalArgumentException | JsonParseException jsonparseexception) {
                LOGGER.error("Parsing error loading campfire effect {}", resourcelocation, jsonparseexception);
            }
        }

        this.campfireEffects = builder.build();
        LOGGER.info("Loaded {} campfire effects", campfireEffects.size());
    }

    public static CampfireEffect fromJson(ResourceLocation id, JsonObject pJson) {
        if (pJson.size() == 0) {
            return null;
        }
        return CampfireEffect.CODEC.decode(JsonOps.INSTANCE,pJson).resultOrPartial(LOGGER::error).orElseThrow().getFirst();
    }
}
