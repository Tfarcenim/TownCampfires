package tfar.towncampfires.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.init.ModItems;
import tfar.towncampfires.init.ModTags;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(DataGenerator pGenerator, BlockTagsProvider pBlockTagsProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(pGenerator, pBlockTagsProvider, TownCampfires.MODID, existingFileHelper);
    }

    @Override
    protected void addTags() {
        tag(ModTags.ALWAYS_USABLE).add(Items.DIRT, ModItems.TOWN_CAMPFIRE_EXPERIENCE);
        tag(ModTags.USABLE_OUTSIDE_OF_CAMPFIRE_RANGE).addTag(ModTags.ALWAYS_USABLE);
        tag(ModTags.WORKBENCHES);
        tag(ModTags.USABLE_WITHIN_CAMPFIRE_RANGE).addTags(ModTags.ALWAYS_USABLE,ModTags.WORKBENCHES).addTag(ItemTags.PLANKS);
    }
}
