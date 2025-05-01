package tfar.towncampfires;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.level.biome.Biome;

public record CampfireEffect(Component name, Component desc, MobEffectCategory category, TagKey<Biome> requiredBiomes,
                             TagKey<Biome> requiredNearbyBiomes, int minLevel, int maxLevel, int weight, MobEffect effect,boolean hidden) {

    

}
