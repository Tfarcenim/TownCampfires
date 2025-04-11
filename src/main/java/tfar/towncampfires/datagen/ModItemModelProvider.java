package tfar.towncampfires.datagen;

import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.init.ModItems;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, TownCampfires.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        generatedItem(ModItems.TOWN_CAMPFIRE_EXPERIENCE,mcLoc("item/heart_of_the_sea"));
    }

    protected void makeSimpleBlockItem(Item item, ResourceLocation loc) {
        String s = Registry.ITEM.getKey(item).toString();
        getBuilder(s)
                .parent(getExistingFile(loc));
    }

    protected void makeSimpleBlockItem(Item item) {
        makeSimpleBlockItem(item,TownCampfires.id("block/" + Registry.ITEM.getKey(item).getPath()));
    }


    private void generatedItem(Item item ,ResourceLocation texture) {
        String path = Registry.ITEM.getKey(item).getPath();
        singleTexture(path, mcLoc("item/generated"),
                "layer0", texture);
    }

    private void generatedItem(Item item) {
        generatedItem(item,modLoc("item/"+Registry.ITEM.getKey(item).getPath()));
    }
}
