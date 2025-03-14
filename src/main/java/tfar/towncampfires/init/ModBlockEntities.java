package tfar.towncampfires.init;

import net.minecraft.world.level.block.entity.BlockEntityType;
import tfar.towncampfires.TownCampfireBlockEntity;

public class ModBlockEntities {

    public static final BlockEntityType<TownCampfireBlockEntity> TOWN_CAMPFIRE = BlockEntityType.Builder.of(TownCampfireBlockEntity::new,
            ModBlocks.GRAY_TOWN_CAMPFIRE,ModBlocks.GREEN_TOWN_CAMPFIRE,ModBlocks.LIGHT_BLUE_TOWN_CAMPFIRE,
            ModBlocks.ORANGE_TOWN_CAMPFIRE,ModBlocks.RED_TOWN_CAMPFIRE).build(null);

}
