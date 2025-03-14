package tfar.towncampfires;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tfar.towncampfires.init.ModBlockEntities;

import javax.annotation.Nullable;

public class TownCampfireBlock extends CampfireBlock {
    public TownCampfireBlock(boolean pSpawnParticles, int pFireDamage, Properties pProperties) {
        super(pSpawnParticles, pFireDamage, pProperties);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide) {
            return pState.getValue(LIT) ? createTickerHelper(pBlockEntityType, ModBlockEntities.TOWN_CAMPFIRE, CampfireBlockEntity::particleTick) : null;
        } else {
            return pState.getValue(LIT) ? createTickerHelper(pBlockEntityType, ModBlockEntities.TOWN_CAMPFIRE, CampfireBlockEntity::cookTick) :
                    createTickerHelper(pBlockEntityType, ModBlockEntities.TOWN_CAMPFIRE, CampfireBlockEntity::cooldownTick);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new TownCampfireBlockEntity(pPos,pState);
    }
}
