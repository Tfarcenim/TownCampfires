package tfar.towncampfires.datagen;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.KilledTrigger;
import net.minecraft.commands.CommandFunction;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.data.quest.Quest;
import tfar.towncampfires.data.quest.QuestCriteria;
import tfar.towncampfires.data.quest.QuestRewards;
import tfar.towncampfires.data.quest.criteria.FailureCriteria;

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
        
        FinishedQuest.builder().name(Component.literal("Example Quest 0").withStyle(ChatFormatting.RED),Component.literal("Ex. Q0"))
                .desc(Component.literal("Example Quest 0 Description").withStyle(ChatFormatting.DARK_GRAY))
                .compactDesc(Component.literal("Ex Q0 Desc").withStyle(ChatFormatting.DARK_GRAY))
                .addCriteria(new QuestCriteria<>(CriteriaTriggers.PLAYER_KILLED_ENTITY, KilledTrigger.TriggerInstance.playerKilledEntity(),
                        Component.literal("Kill Mobs").withStyle(ChatFormatting.DARK_GRAY)),count)
                .addStages("Quest 1")
                .rewards(
                        new QuestRewards(100,100,new ResourceLocation[0], new ResourceLocation[0], CommandFunction.CacheableFunction.NONE,
                                new String[]{"Completed Quest 1"},new String[]{"Quest 1"}, List.of(new MobEffectInstance(MobEffects.REGENERATION,600)),List.of()))
                .punishment(
                        new QuestRewards(-1000,0,new ResourceLocation[0], new ResourceLocation[0], CommandFunction.CacheableFunction.NONE,
                                new String[]{"Failed Quest 1"},new String[]{"Quest 1"}, List.of(new MobEffectInstance(MobEffects.POISON,600)),List.of()))
                .save(consumer, TownCampfires.id("example_quest_0"));

        FinishedQuest.builder().name(Component.literal("Example Quest 1").withStyle(ChatFormatting.GOLD),null)
                .desc(Component.literal("Example Quest 1 Description").withStyle(ChatFormatting.DARK_GRAY))
                .addCriteria(new QuestCriteria<>(CriteriaTriggers.PLAYER_KILLED_ENTITY,
                        KilledTrigger.TriggerInstance.playerKilledEntity(), Component.literal("Kill Mobs").withStyle(ChatFormatting.DARK_GRAY)),count)
                .save(consumer, TownCampfires.id("example_quest_1"));

        FinishedQuest.builder().name(Component.literal("Don't kill Creepers").withStyle(ChatFormatting.GOLD),null)
                .desc(Component.literal("Don't kill any creepers").withStyle(ChatFormatting.DARK_GRAY))
                .slots(10)
                .attempts(1)
                .type(Quest.MultiplayerType.prep_solo)
                .addCriteria(new QuestCriteria<>(CriteriaTriggers.PLAYER_KILLED_ENTITY,
                        KilledTrigger.TriggerInstance.playerKilledEntity(), Component.literal("Kill Mobs").withStyle(ChatFormatting.DARK_GRAY)),count)
                .addFailureCriteria(new QuestCriteria<>(CriteriaTriggers.PLAYER_KILLED_ENTITY,
                        KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(EntityType.CREEPER)),
                        Component.literal("Avoid Creeper").withStyle(ChatFormatting.DARK_GRAY)))
                .punishment(
                        new QuestRewards(-1000,0,new ResourceLocation[0], new ResourceLocation[0], CommandFunction.CacheableFunction.NONE,
                                new String[0],new String[0], List.of(new MobEffectInstance(MobEffects.POISON,600)),List.of()))
                .save(consumer, TownCampfires.id("dont_kill_creepers"));

        FinishedQuest.builder().name(Component.literal("Don't die").withStyle(ChatFormatting.GOLD),null)
                .desc(Component.literal("Kill zombie without dying").withStyle(ChatFormatting.DARK_GRAY))
                .slots(1)
                .attempts(1)
                .type(Quest.MultiplayerType.prep_solo)
                .addCriteria(new QuestCriteria<>(CriteriaTriggers.PLAYER_KILLED_ENTITY,
                        KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(EntityType.CREEPER)),
                        Component.literal("Kill Zombie").withStyle(ChatFormatting.DARK_GRAY)),count)
                .setFailureCriteria(new FailureCriteria(new ArrayList<>(),true,1000,new ArrayList<>(),new ArrayList<>()))
                .punishment(
                        new QuestRewards(-1000,0,new ResourceLocation[0], new ResourceLocation[0], CommandFunction.CacheableFunction.NONE,
                                new String[0],new String[0], List.of(new MobEffectInstance(MobEffects.POISON,600)),List.of()))
                .save(consumer, TownCampfires.id("kill_zombie"));

        /*FinishedQuest.builder().title(Component.literal("Stay near Campfire").withStyle(ChatFormatting.GOLD))
                .desc(Component.literal("Stay near campfire").withStyle(ChatFormatting.DARK_GRAY),
                        Component.literal("for 1 minute").withStyle(ChatFormatting.DARK_GRAY))
                .addCriteria(new QuestCriteria<>(CriteriaTriggers.TICK,
                        PlayerTrigger.TriggerInstance.located(), Component.literal("Kill Mobs").withStyle(ChatFormatting.DARK_GRAY)),count)
                .addFailureCriteria(new QuestCriteria<>(CriteriaTriggers.PLAYER_KILLED_ENTITY,
                        KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(EntityType.CREEPER)),
                        Component.literal("Avoid Creeper").withStyle(ChatFormatting.DARK_GRAY)))
                .punishment(new QuestPunishments(-1000,0,new ResourceLocation[0], new ResourceLocation[0], CommandFunction.CacheableFunction.NONE))
                .save(consumer, TownCampfires.id("dont_kill_creepers"));*/


        FinishedQuest.builder().name(Component.literal("Example Level Quest 0").withStyle(ChatFormatting.RED), Component.literal("Ex. Lvl Q0"))
                .markLevelUp()
                .desc(Component.literal("Example Level Quest 0 Desc").withStyle(ChatFormatting.DARK_GRAY))
                .compactDesc(Component.literal("Ex. Lvl Q0 Desc").withStyle(ChatFormatting.DARK_GRAY))
                .addCriteria(new QuestCriteria<>(CriteriaTriggers.PLAYER_KILLED_ENTITY,
                        KilledTrigger.TriggerInstance.playerKilledEntity(), Component.literal("Kill Mobs").withStyle(ChatFormatting.DARK_GRAY)),count)
                .save(consumer, TownCampfires.id("example_level_quest_0"));

    }

    @Override
    public String getName() {
        return "Quests";
    }

}
