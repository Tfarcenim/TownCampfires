package tfar.towncampfires;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tfar.towncampfires.init.ModBlockEntities;

public class TownCampfireBlockEntity extends CampfireBlockEntity {

    protected TownCampfire townCampfire;

    public TownCampfireBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(pPos, pBlockState);
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.TOWN_CAMPFIRE;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
    }

    public void setLinkedCampfire(TownCampfire campfire) {
        townCampfire = campfire;
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, TownCampfireBlockEntity pBlockEntity) {
        if (pState.getValue(CampfireBlock.LIT)){
            CampfireBlockEntity.cookTick(pLevel, pPos, pState, pBlockEntity);
            extraServerTick(pLevel, pPos, pState, pBlockEntity);
        }
        else CampfireBlockEntity.cooldownTick(pLevel, pPos, pState, pBlockEntity);
    }

    public static void extraServerTick(Level pLevel, BlockPos pPos, BlockState pState, TownCampfireBlockEntity pBlockEntity) {
        TownCampfire townCampfire = pBlockEntity.townCampfire;

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
