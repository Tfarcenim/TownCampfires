package tfar.towncampfires.utils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import tfar.towncampfires.CampfireEffect;
import tfar.towncampfires.TownCampfires;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static List<ResourceLocation> pickEffects(RandomSource random,List<ResourceLocation> eligible, int max) {
        MutableWeightedRandomList<WeightedEntry.Wrapper<CampfireEffect>> builder = MutableWeightedRandomList.create();

        for (ResourceLocation location : eligible) {
            CampfireEffect effect = TownCampfires.campfireEffectLoader.getCampfireEffects().get(location);
            if (effect != null) {
                builder.add(WeightedEntry.wrap(effect,effect.weight()));
            }
        }

        List<ResourceLocation> sampled = new ArrayList<>();

        for (int i = 0 ; i < max;i++) {
            builder.getRandom(random).ifPresent(campfireEffectWrapper -> {
                sampled.add(TownCampfires.campfireEffectLoader.lookup(campfireEffectWrapper.getData()));
                builder.remove(campfireEffectWrapper);
            });
            if (eligible.isEmpty()) break;
        }
        return sampled;
    }
}
