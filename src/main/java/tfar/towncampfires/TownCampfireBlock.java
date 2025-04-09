package tfar.towncampfires;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import tfar.towncampfires.client.TownCampfiresClient;
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
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if (itemstack.isEmpty()) {
            if (pLevel.isClientSide) {
                TownCampfiresClient.openCampfireScreen(pPlayer);
            }
            return InteractionResult.SUCCESS;
        }else {
            return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new TownCampfireBlockEntity(pPos,pState);
    }
}
