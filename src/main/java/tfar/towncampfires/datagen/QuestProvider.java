package tfar.towncampfires.datagen;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.KilledTrigger;
import net.minecraft.advancements.critereon.PlacedBlockTrigger;
import net.minecraft.commands.CommandFunction;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.data.quest.QuestCriteria;
import tfar.towncampfires.data.quest.QuestRewards;
import tfar.towncampfires.data.quest.criteria.Delivery;
import tfar.towncampfires.data.quest.criteria.SuccessCriteria;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
        buildQuests(finishedQuest -> {
            if (!set.add(finishedQuest.getId())) {
                throw new IllegalStateException("Duplicate quest " + finishedQuest.getId());
            } else {
                saveQuest(pOutput, finishedQuest.serialize(), this.pathProvider.json(finishedQuest.getId()));
            }
        });
    }

    private static void saveQuest(CachedOutput pOutput, JsonObject json, Path pPath) {
        try {
            DataProvider.saveStable(pOutput, json, pPath);
        } catch (IOException ioexception) {
            LOGGER.error("Couldn't save quest {}", pPath, ioexception);
        }

    }

    protected void buildQuests(Consumer<FinishedQuest> consumer) {
        
        int count = 2;

        List<QuestCriteria<?>> successCriteria = List.of(new QuestCriteria<>(CriteriaTriggers.PLAYER_KILLED_ENTITY, KilledTrigger.TriggerInstance.playerKilledEntity(),
                                Component.literal("Kill Mobs").withStyle(ChatFormatting.DARK_GRAY),count));

        FinishedQuest.builder().name(Component.literal("Kill Mobs").withStyle(ChatFormatting.RED))
                .desc(Component.literal("Kill any 2 mobs").withStyle(ChatFormatting.DARK_GRAY))
                .setSuccessCriteria(new SuccessCriteria(successCriteria,new ArrayList<>()))
                .rewards(
                        new QuestRewards(100,100,new ResourceLocation[0], new ResourceLocation[0], CommandFunction.CacheableFunction.NONE,
                                new String[]{"Completed Kill Mobs"},new String[]{"Kill Mobs"}, List.of(new MobEffectInstance(MobEffects.REGENERATION,600)),List.of()))
                .punishment(
                        new QuestRewards(-1000,0,new ResourceLocation[0], new ResourceLocation[0], CommandFunction.CacheableFunction.NONE,
                                new String[]{"Failed Kill Mobs"},new String[]{"Kill Mobs"}, List.of(new MobEffectInstance(MobEffects.POISON,600)),List.of()))
                .save(consumer, TownCampfires.id("kill_mobs"));

        List<QuestCriteria<?>> placeCriteria = List.of(new QuestCriteria<>(CriteriaTriggers.PLACED_BLOCK, PlacedBlockTrigger.TriggerInstance.placedBlock(Blocks.OAK_FENCE),
                        Component.literal("Place oak fences").withStyle(ChatFormatting.DARK_GRAY),3));

        FinishedQuest.builder().name(Component.literal("Place fence").withStyle(ChatFormatting.RED))
                .desc(Component.literal("Place some oak fences").withStyle(ChatFormatting.DARK_GRAY))
                .setSuccessCriteria(new SuccessCriteria(placeCriteria,new ArrayList<>()))
                .rewards(
                        new QuestRewards(100,100,new ResourceLocation[0], new ResourceLocation[0], CommandFunction.CacheableFunction.NONE,
                                new String[]{},new String[]{}, List.of(new MobEffectInstance(MobEffects.REGENERATION,600)),List.of()))
                .save(consumer, TownCampfires.id("place_oak_fences"));


        FinishedQuest.builder().name(Component.literal("Chop wood").withStyle(ChatFormatting.RED))
                .icon(Blocks.OAK_LOG.asItem().getDefaultInstance())
                .desc(Component.literal("Chop down some trees").withStyle(ChatFormatting.DARK_GRAY))
                .setSuccessCriteria(new SuccessCriteria(new ArrayList<>(),List.of(new Delivery(Ingredient.of(ItemTags.LOGS),8))))
                .rewards(
                        new QuestRewards(100,100,new ResourceLocation[0], new ResourceLocation[0], CommandFunction.CacheableFunction.NONE,
                                new String[]{},new String[]{}, List.of(new MobEffectInstance(MobEffects.REGENERATION,600)),List.of()))
                .save(consumer, TownCampfires.id("chop_wood"));

    }

    @Override
    public String getName() {
        return "Quests";
    }

}
