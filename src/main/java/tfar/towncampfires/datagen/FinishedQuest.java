package tfar.towncampfires.datagen;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.data.quest.Quest;

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

        public void save(Consumer<FinishedQuest> consumer, ResourceLocation id) {
            consumer.accept(build(id));
        }

        private FinishedQuest build(ResourceLocation id) {
            return new FinishedQuest(id,new Quest(title,icon,desc));
        }
    }


    /**
     * Gets the JSON for the recipe.
     */
    JsonObject serialize() {
        JsonObject jsonobject = quest.CODEC.encodeStart(JsonOps.INSTANCE, quest).resultOrPartial(TownCampfires.LOGGER::error).get().getAsJsonObject();
        return jsonobject;
    }

    /**
     * Gets the ID for the recipe.
     */
    ResourceLocation getId() {
        return id;
    }
}
