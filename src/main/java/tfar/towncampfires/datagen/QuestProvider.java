package tfar.towncampfires.datagen;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import tfar.towncampfires.TownCampfires;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

public class QuestProvider implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final DataGenerator.PathProvider pathProvider;

    public QuestProvider(DataGenerator pGenerator) {
        this.pathProvider = pGenerator.createPathProvider(DataGenerator.Target.DATA_PACK, "quests");
    }

    @Override
    public void run(CachedOutput pOutput) {
        Set<ResourceLocation> set = Sets.newHashSet();
        buildQuests(finishedCampfireEffect -> {
            if (!set.add(finishedCampfireEffect.getId())) {
                throw new IllegalStateException("Duplicate quest " + finishedCampfireEffect.getId());
            } else {
                saveCampfireEffect(pOutput, finishedCampfireEffect.serialize(), this.pathProvider.json(finishedCampfireEffect.getId()));
            }
        });
    }

    private static void saveCampfireEffect(CachedOutput pOutput, JsonObject json, Path pPath) {
        try {
            DataProvider.saveStable(pOutput, json, pPath);
        } catch (IOException ioexception) {
            LOGGER.error("Couldn't save quest {}", pPath, ioexception);
        }

    }

    protected void buildQuests(Consumer<FinishedQuest> consumer) {
        FinishedQuest.builder().title(Component.literal("Example Quest 0"))
                .desc(Component.literal("Example Quest 0 Description"))
                .save(consumer, TownCampfires.id("example_quest_0"));

        FinishedQuest.builder().title(Component.literal("Example Quest 1"))
                .desc(Component.literal("Example Quest 1 Description"))
                .save(consumer, TownCampfires.id("example_quest_1"));

        FinishedQuest.builder().title(Component.literal("Example Quest 2"))
                .desc(Component.literal("Example Quest 2 Description"))
                .save(consumer, TownCampfires.id("example_quest_2"));

        FinishedQuest.builder().title(Component.literal("Example Quest 3"))
                .desc(Component.literal("Example Quest 3 Description"))
                .save(consumer, TownCampfires.id("example_quest_3"));

        FinishedQuest.builder().title(Component.literal("Example Quest 4"))
                .desc(Component.literal("Example Quest 4 Description"))
                .save(consumer, TownCampfires.id("example_quest_4"));

    }

    @Override
    public String getName() {
        return "Quests";
    }

}
