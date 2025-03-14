package tfar.towncampfires.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfar.towncampfires.CampfireStructurePoolElement;

import java.util.HashSet;
import java.util.Set;

/**
 * borrowed from Waystones
 */
@Mixin(SinglePoolElement.class)
public abstract class SinglePoolElementMixin implements CampfireStructurePoolElement {

    @Shadow
    public abstract String toString();

    private static final Set<BlockPos> generatedCampfires = new HashSet<>();

    private Boolean isCampfire;

    @Override
    public boolean isCampfire() {
        if (isCampfire == null) {
            isCampfire = toString().contains("town_campfire");
        }
        return isCampfire;
    }

    @Override
    public void setIsCampfire(boolean isCampfire) {
        this.isCampfire = isCampfire;
    }

    @Inject(method = "place(Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Rotation;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/util/RandomSource;Z)Z", at = @At("HEAD"), cancellable = true)
    public void place(StructureTemplateManager structureTemplateManager, WorldGenLevel worldGenLevel, StructureManager structureManager, ChunkGenerator chunkGenerator, BlockPos blockPos, BlockPos blockPos2, Rotation rotation, BoundingBox boundingBox, RandomSource randomSource, boolean bl, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (isCampfire()) {
            for (BlockPos existingPos : generatedCampfires) {
                // place is called separately for waystones crossing chunk borders, but the two blockpos parameters will be unique per generated waystone
                // therefore, only block nearby waystones if it's not literally the waystone that's supposed to be blocking it
                // future blay will smh at past blay when this breaks due to relying on an identity check instead of comparing the BlockPos values
                if (blockPos != existingPos && existingPos.distSqr(blockPos) < 100 * 100) {
                    callbackInfo.setReturnValue(false);
                    return;
                }
            }
            generatedCampfires.add(blockPos);
        }
    }
}