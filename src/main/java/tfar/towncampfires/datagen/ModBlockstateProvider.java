package tfar.towncampfires.datagen;

import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.init.ModBlocks;

public class ModBlockstateProvider extends BlockStateProvider {
    public ModBlockstateProvider(DataGenerator gen, ExistingFileHelper exFileHelper) {
        super(gen, TownCampfires.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        ModelFile.ExistingModelFile campfire_off = models().getExistingFile(mcLoc("block/campfire_off"));
        ResourceLocation campfire = new ResourceLocation("block/campfire");

        DyeColor[] colors = new DyeColor[]{DyeColor.ORANGE,DyeColor.RED,DyeColor.LIGHT_BLUE,DyeColor.GREEN,DyeColor.GRAY};

        for (DyeColor color : colors){
            Block b = Registry.BLOCK.get(TownCampfires.id(color+"_town_campfire"));
            getVariantBuilder(b).forAllStatesExcept(state -> {
                        Direction facing = state.getValue(CampfireBlock.FACING);
                        boolean lit = state.getValue(CampfireBlock.LIT);
                        ModelFile modelFile;
                        if (lit) {
                            modelFile = models().withExistingParent(color+"_campfire",campfire).texture("fire",modLoc("block/"+color+"_campfire_fire"));
                        } else {
                            modelFile = campfire_off;
                        }

                        int rotation = (int) facing.toYRot();

                        return ConfiguredModel.builder().modelFile(modelFile).rotationY(rotation).build();
                    }
                    , CampfireBlock.SIGNAL_FIRE,CampfireBlock.WATERLOGGED);
        }
    }
}
