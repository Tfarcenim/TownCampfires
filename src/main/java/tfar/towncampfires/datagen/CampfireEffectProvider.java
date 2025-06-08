package tfar.towncampfires.datagen;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.slf4j.Logger;
import tfar.towncampfires.TownCampfires;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

public class CampfireEffectProvider implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final DataGenerator.PathProvider pathProvider;

    public CampfireEffectProvider(DataGenerator pGenerator) {
        this.pathProvider = pGenerator.createPathProvider(DataGenerator.Target.DATA_PACK, "campfire_effects");
    }

    @Override
    public void run(CachedOutput pOutput) {
        Set<ResourceLocation> set = Sets.newHashSet();
        buildCampfireEffects(finishedCampfireEffect -> {
            if (!set.add(finishedCampfireEffect.getId())) {
                throw new IllegalStateException("Duplicate campfire effect " + finishedCampfireEffect.getId());
            } else {
                saveCampfireEffect(pOutput, finishedCampfireEffect.serialize(), this.pathProvider.json(finishedCampfireEffect.getId()));
            }
        });
    }

    private static void saveCampfireEffect(CachedOutput pOutput, JsonObject json, Path pPath) {
        try {
            DataProvider.saveStable(pOutput, json, pPath);
        } catch (IOException ioexception) {
            LOGGER.error("Couldn't save campfire effect {}", pPath, ioexception);
        }

    }

    protected void buildCampfireEffects(Consumer<FinishedCampfireEffect> consumer) {
        FinishedCampfireEffect.builder().title(Component.literal("Regeneration Effect"))
                .desc(Component.literal("Positive effect 0 desc"))
                .levelRange(0,5).save(consumer, TownCampfires.id("example_positive_effect_0"));

        FinishedCampfireEffect.builder().title(Component.literal("Fire Resistance Effect"))
                .desc(Component.literal("Positive effect 1 desc"))
                .effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,100))
                .levelRange(0,5).save(consumer, TownCampfires.id("example_positive_effect_1"));

       /* FinishedCampfireEffect.builder().title(Component.literal("Example Negative Effect 0"))
                .desc(Component.literal("line 0"),Component.literal("line 1"))
                .levelRange(0,5)
                .category(MobEffectCategory.HARMFUL)
                .save(consumer, TownCampfires.id("example_negative_effect_0"));

        FinishedCampfireEffect.builder().title(Component.literal("Example Negative Effect 1"))
                .desc(Component.literal("line 0"),Component.literal("line 1"))
                .levelRange(0,5)
                .category(MobEffectCategory.HARMFUL)
                .save(consumer, TownCampfires.id("example_negative_effect_1"));*/

    }

    @Override
    public String getName() {
        return "Campfire Effects";
    }

}
