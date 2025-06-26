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
import tfar.towncampfires.data.quest.Quest;
import tfar.towncampfires.data.quest.QuestAppearanceConditions;
import tfar.towncampfires.data.quest.QuestCriteria;
import tfar.towncampfires.data.quest.QuestRewards;

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
        Quest.Type type = Quest.Type.normal;

        List<Pair<QuestCriteria<?>,Integer>> criteria = new ArrayList<>();

        QuestRewards rewards = new QuestRewards(1, new ResourceLocation[0], new ResourceLocation[0], CommandFunction.CacheableFunction.NONE);

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

        public Builder type(Quest.Type type) {
            this.type = type;
            return this;
        }

        public Builder addCriteria(QuestCriteria<?> criteria) {
            this.criteria.add(Pair.of(criteria,1));
            return this;
        }

        public Builder addCriteria(QuestCriteria<?> criteria,int count) {
            this.criteria.add(Pair.of(criteria,count));
            return this;
        }

        public Builder rewards(QuestRewards rewards) {
            this.rewards = rewards;
            return this;
        }

        public void save(Consumer<FinishedQuest> consumer, ResourceLocation id) {
            consumer.accept(build(id));
        }

        private FinishedQuest build(ResourceLocation id) {
            return new FinishedQuest(id,new Quest(title,icon,desc,questAppearanceConditions,type,criteria,rewards));
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
