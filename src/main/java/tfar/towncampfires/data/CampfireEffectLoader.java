package tfar.towncampfires.data;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CampfireEffectLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private Map<ResourceLocation, CampfireEffect> campfireEffects = ImmutableMap.of();
    public CampfireEffectLoader() {
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

    public List<ResourceLocation> getEligibleEffects(int level, MobEffectCategory category, Holder<Biome> biome, HolderSet<Biome> nearbyBiomes) {

        List<ResourceLocation> list = new ArrayList<>();
        for(Map.Entry<ResourceLocation,CampfireEffect> entry : campfireEffects.entrySet()) {
            CampfireEffect campfireEffect = entry.getValue();

            boolean inRange = campfireEffect.levelRange().test(level);
            if (!inRange) continue;

            boolean hasNearby = false;
            for (Holder<Biome> biomeHolder : nearbyBiomes) {
                if (biomeHolder.is(campfireEffect.requiredNearbyBiomes())) {
                    hasNearby = true;
                    break;
                }
            }

            if (hasNearby && campfireEffect.category() == category && biome.is(campfireEffect.requiredBiomes())) {
                list.add(entry.getKey());
            }
        }
        return list;
    }

    public Map<ResourceLocation, CampfireEffect> getCampfireEffects() {
        return campfireEffects;
    }

    public Map<ResourceLocation,CampfireEffect> getNonHiddenEffects() {
        Map<ResourceLocation,CampfireEffect> copy = new HashMap<>(campfireEffects);
        copy.entrySet().removeIf(resourceLocationCampfireEffectEntry -> resourceLocationCampfireEffectEntry.getValue().hidden());
        return copy;
    }

    public ResourceLocation lookup(CampfireEffect effect) {
        return campfireEffects.keySet().stream()
                .filter(resourceLocationCampfireEffectEntry -> campfireEffects.get(resourceLocationCampfireEffectEntry) == effect).findFirst().orElse(null);
    }

    public static CampfireEffect fromJson(ResourceLocation id, JsonObject pJson) {
        if (pJson.size() == 0) {
            return null;
        }
        return CampfireEffect.CODEC.decode(JsonOps.INSTANCE,pJson).resultOrPartial(LOGGER::error).orElseThrow().getFirst();
    }

    public void setFromServer(Map<ResourceLocation,CampfireEffect> effectMap) {
        this.campfireEffects = effectMap;
    }
}
