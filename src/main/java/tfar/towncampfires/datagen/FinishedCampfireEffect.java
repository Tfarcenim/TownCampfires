package tfar.towncampfires.datagen;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.biome.Biome;
import tfar.towncampfires.CampfireEffect;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.config.IntegerRange;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class FinishedCampfireEffect {

    private final ResourceLocation id;
    private final CampfireEffect effect;

    protected FinishedCampfireEffect(ResourceLocation id, CampfireEffect effect) {
        this.id = id;
        this.effect = effect;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        Component title= Component.literal("Title");
        List<Component> desc;
        MobEffectCategory category = MobEffectCategory.BENEFICIAL;
        TagKey<Biome> requiredBiomes = BiomeTags.IS_OVERWORLD;
        TagKey<Biome> requiredNearbyBiomes = BiomeTags.IS_OVERWORLD;
        IntegerRange levelRange;
        int weight = 1;
        MobEffect effect = MobEffects.REGENERATION;
        boolean hidden;

        public Builder title(Component title) {
            this.title = title;
            return this;
        }

        public Builder desc(Component... desc) {
            this.desc = Arrays.stream(desc).toList();
            return this;
        }

        public Builder category(MobEffectCategory category) {
            this.category = category;
            return this;
        }

        public Builder levelRange(int min,int max) {
            levelRange = IntegerRange.inclusive(min,max);
            return this;
        }

        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public Builder effect(MobEffect effect) {
            this.effect =effect;
            return this;
        }

        public void save(Consumer<FinishedCampfireEffect> consumer, ResourceLocation id) {
            consumer.accept(build(id));
        }

        private FinishedCampfireEffect build(ResourceLocation id) {
            return new FinishedCampfireEffect(id,new CampfireEffect(title,desc,category,requiredBiomes,requiredNearbyBiomes,levelRange,weight,effect,hidden));
        }
    }


    /**
     * Gets the JSON for the recipe.
     */
    JsonObject serialize() {
        JsonObject jsonobject = CampfireEffect.CODEC.encodeStart(JsonOps.INSTANCE, effect).resultOrPartial(TownCampfires.LOGGER::error).get().getAsJsonObject();
        return jsonobject;
    }

    /**
     * Gets the ID for the recipe.
     */
    ResourceLocation getId() {
        return id;
    }
}
