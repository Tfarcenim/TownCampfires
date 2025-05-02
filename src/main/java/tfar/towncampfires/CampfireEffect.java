package tfar.towncampfires;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.level.biome.Biome;
import tfar.towncampfires.config.IntegerRange;
import tfar.towncampfires.utils.MiscCodecs;

import java.util.List;

public record CampfireEffect(Component name, List<Component> desc, MobEffectCategory category, TagKey<Biome> requiredBiomes,
                             TagKey<Biome> requiredNearbyBiomes, IntegerRange levelRange, int weight, MobEffect effect, boolean hidden) {

    public static final Codec<CampfireEffect> CODEC = RecordCodecBuilder.create(campfireEffectInstance -> campfireEffectInstance
            .group(
                    MiscCodecs.COMPONENT_CODEC.fieldOf("name").forGetter(CampfireEffect::name),
                    MiscCodecs.COMPONENT_CODEC.listOf().fieldOf("desc").forGetter(CampfireEffect::desc),
                    MiscCodecs.MOB_EFFECT_CATEGORY_CODEC.fieldOf("category").forGetter(CampfireEffect::category),
                    TagKey.codec(Registry.BIOME_REGISTRY).fieldOf("required_biomes").forGetter(CampfireEffect::requiredBiomes),
                    TagKey.codec(Registry.BIOME_REGISTRY).fieldOf("required_nearby_biomes").forGetter(CampfireEffect::requiredNearbyBiomes),
                    IntegerRange.CODEC.fieldOf("level_range").forGetter(CampfireEffect::levelRange),
                    Codec.INT.fieldOf("weight").forGetter(CampfireEffect::weight),
                    Registry.MOB_EFFECT.byNameCodec().fieldOf("effect").forGetter(CampfireEffect::effect),
                    Codec.BOOL.fieldOf("hidden").forGetter(CampfireEffect::hidden)
                    ).apply(campfireEffectInstance,CampfireEffect::new)

    );

}
