package tfar.towncampfires;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tfar.towncampfires.init.ModBlockEntities;

public class TownCampfireBlockEntity extends CampfireBlockEntity {
    public TownCampfireBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(pPos, pBlockState);
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.TOWN_CAMPFIRE;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            CampfireLevelData campfireLevelData = CampfireLevelData.getOrCreate((ServerLevel) level);
            campfireLevelData.save(this);
        }
    }
}
