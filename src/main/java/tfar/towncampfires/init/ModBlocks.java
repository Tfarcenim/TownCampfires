package tfar.towncampfires.init;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import tfar.towncampfires.TownCampfireBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

public class ModBlocks {

    public static final List<DyeColor> CAMPFIRE_COLORS = new ArrayList<>();

    static {
        CAMPFIRE_COLORS.add(DyeColor.RED);
        CAMPFIRE_COLORS.add(DyeColor.ORANGE);
        CAMPFIRE_COLORS.add(DyeColor.LIGHT_BLUE);
        CAMPFIRE_COLORS.add(DyeColor.GREEN);
        CAMPFIRE_COLORS.add(DyeColor.GRAY);
    }

    public static final Block ORANGE_TOWN_CAMPFIRE = new TownCampfireBlock(true, 1, properties());
    public static final Block RED_TOWN_CAMPFIRE = new TownCampfireBlock(true, 1, properties());
    public static final Block LIGHT_BLUE_TOWN_CAMPFIRE = new TownCampfireBlock(true, 1, properties());
    public static final Block GREEN_TOWN_CAMPFIRE = new TownCampfireBlock(true, 1, properties());
    public static final Block GRAY_TOWN_CAMPFIRE = new TownCampfireBlock(true, 1, properties());

    public static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.PODZOL)
                .strength(-1, 3600000)
                .sound(SoundType.WOOD)
                .lightLevel(litBlockEmission(15))
                .noOcclusion();
    }

    private static ToIntFunction<BlockState> litBlockEmission(int pLightValue) {
        return (p_50763_) -> p_50763_.getValue(BlockStateProperties.LIT) ? pLightValue : 0;
    }
}
