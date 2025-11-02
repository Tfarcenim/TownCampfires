package tfar.towncampfires.datagen;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import tfar.towncampfires.config.IntegerRange;
import tfar.towncampfires.data.quest.*;
import tfar.towncampfires.data.quest.criteria.FailureCriteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class FinishedQuest {

    private final ResourceLocation id;
    private final Quest quest;

    protected FinishedQuest(ResourceLocation id, Quest quest) {
        this.id = id;
        this.quest = quest;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        Component name = Component.literal("Title");
        Component compact_name = name;
        ItemStack icon = Items.GOLD_INGOT.getDefaultInstance();
        List<Component> desc;
        List<Component> compact_desc;
        QuestAppearanceConditions questAppearanceConditions = new QuestAppearanceConditions(BiomeTags.IS_OVERWORLD,
                true,BiomeTags.IS_OVERWORLD, IntegerRange.inclusive(0,Integer.MAX_VALUE),1,IntegerRange.inclusive(0,Integer.MAX_VALUE));
        Quest.MultiplayerType multiplayerType = Quest.MultiplayerType.solo;

        List<Pair<QuestCriteria<?>,Integer>> criteria = new ArrayList<>();

        FailureCriteria failureCriteria = FailureCriteria.EMPTY;

        QuestRewards rewards = new QuestRewards(100,100, new ResourceLocation[0], new ResourceLocation[0], CommandFunction.CacheableFunction.NONE,
                new String[0],new String[0],List.of(),List.of());

        QuestRewards punishments = QuestRewards.EMPTY;
        boolean levelUp;

        long weight = 1;
        int difficulty = 1;
        int slots = 1;
        int attempts = 1;
        final List<String> stages = new ArrayList<>();

        public Builder name(Component name, @Nullable Component compactName) {
            this.name = name;
            compact_name = compactName != null ? compactName : name;
            return this;
        }

        public Builder icon(ItemStack icon) {
            this.icon = icon;
            return this;
        }

        public Builder desc(Component... desc) {
            this.desc = Arrays.stream(desc).toList();
            compact_desc = this.desc;
            return this;
        }

        public Builder compactDesc(Component... desc) {
            this.compact_desc = Arrays.stream(desc).toList();
            return this;
        }

        public Builder appearanceConditions(QuestAppearanceConditions questAppearanceConditions) {
            this.questAppearanceConditions = questAppearanceConditions;
            return this;
        }

        public Builder type(Quest.MultiplayerType multiplayerType) {
            this.multiplayerType = multiplayerType;
            return this;
        }

        public Builder markLevelUp() {
            levelUp = true;
            return this;
        }

        public Builder addCriteria(QuestCriteria<?> criteria) {
            return addCriteria(criteria,1);
        }

        public Builder addCriteria(QuestCriteria<?> criteria,int count) {
            this.criteria.add(Pair.of(criteria,count));
            return this;
        }

        public Builder rewards(QuestRewards rewards) {
            this.rewards = rewards;
            return this;
        }

        public Builder addFailureCriteria(QuestCriteria<?> criteria) {
            return addFailureCriteria(criteria,1);
        }

        public Builder addFailureCriteria(QuestCriteria<?> criteria,int count) {

            this.failureCriteria.custom().add(Pair.of(criteria,count));
            return this;
        }

        public Builder setFailureCriteria(FailureCriteria failureCriteria) {
            this.failureCriteria = failureCriteria;
            return this;
        }

        public Builder punishment(QuestRewards punishments) {
            this.punishments = punishments;
            return this;
        }

        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public Builder difficulty(int difficulty) {
            this.difficulty = difficulty;
            return this;
        }

        public Builder slots(int slots) {
            this.slots = slots;
            return this;
        }

        public Builder attempts(int attempts) {
            this.attempts = attempts;
            return this;
        }

        public Builder addStages(String... stages) {
            Collections.addAll(this.stages, stages);
            return this;
        }

        public void save(Consumer<FinishedQuest> consumer, ResourceLocation id) {
            consumer.accept(build(id));
        }

        private FinishedQuest build(ResourceLocation id) {
            return new FinishedQuest(id,new Quest(name,icon,desc,questAppearanceConditions, multiplayerType,
                    criteria,rewards, failureCriteria,punishments,levelUp,weight, compact_name,compact_desc,difficulty,slots,attempts,stages));
        }
    }


    /**
     * Gets the JSON for the recipe.
     */
    JsonObject serialize() {
        JsonObject jsonobject = quest.write();
        return jsonobject;
    }

    /**
     * Gets the ID for the recipe.
     */
    ResourceLocation getId() {
        return id;
    }
}
