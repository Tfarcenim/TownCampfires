package tfar.towncampfires.datagen;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tfar.towncampfires.config.IntegerRange;
import tfar.towncampfires.data.quest.*;

import java.util.ArrayList;
import java.util.Arrays;
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

        Component title= Component.literal("Title");
        ItemStack icon = Items.GOLD_INGOT.getDefaultInstance();
        List<Component> desc;
        QuestAppearanceConditions questAppearanceConditions = new QuestAppearanceConditions(BiomeTags.IS_OVERWORLD,
                true,BiomeTags.IS_OVERWORLD, IntegerRange.inclusive(0,Integer.MAX_VALUE),1,IntegerRange.inclusive(0,Integer.MAX_VALUE));
        Quest.MultiplayerType multiplayerType = Quest.MultiplayerType.solo;

        List<Pair<QuestCriteria<?>,Integer>> criteria = new ArrayList<>();

        List<Pair<QuestCriteria<?>,Integer>> failure_criteria = new ArrayList<>();

        QuestRewards rewards = new QuestRewards(100,100, new ResourceLocation[0], new ResourceLocation[0], CommandFunction.CacheableFunction.NONE);

        QuestPunishments punishments = QuestPunishments.EMPTY;
        boolean levelUp;

        public Builder title(Component title) {
            this.title = title;
            return this;
        }

        public Builder icon(ItemStack icon) {
            this.icon = icon;
            return this;
        }

        public Builder desc(Component... desc) {
            this.desc = Arrays.stream(desc).toList();
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
            this.failure_criteria.add(Pair.of(criteria,count));
            return this;
        }

        public Builder punishment(QuestPunishments punishments) {
            this.punishments = punishments;
            return this;
        }

        public void save(Consumer<FinishedQuest> consumer, ResourceLocation id) {
            consumer.accept(build(id));
        }

        private FinishedQuest build(ResourceLocation id) {
            return new FinishedQuest(id,new Quest(title,icon,desc,questAppearanceConditions, multiplayerType,criteria,rewards,failure_criteria,punishments,levelUp));
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
