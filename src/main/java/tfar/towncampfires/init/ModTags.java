package tfar.towncampfires.init;

import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import tfar.towncampfires.TownCampfires;

public class ModTags {
    public static final TagKey<Item> USABLE_WITHIN_CAMPFIRE_RANGE = mod("usable_within_campfire_range");
    public static final TagKey<Item> USABLE_OUTSIDE_OF_CAMPFIRE_RANGE = mod("usable_outside_of_campfire_range");
    public static final TagKey<Item> ALWAYS_USABLE = mod("always_usable");
    public static final TagKey<Item> WORKBENCHES = mod("workbenches");

    public static TagKey<Item> mod(String path) {
        return TagKey.create(Registry.ITEM_REGISTRY, TownCampfires.id(path));
    }
}
