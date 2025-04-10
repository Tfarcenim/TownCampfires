package tfar.towncampfires.mixin.debug;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import tfar.towncampfires.Hooks;

import java.util.Deque;
import java.util.List;

@Mixin(JigsawPlacement.Placer.class)
public class PlacerMixin {
    @Shadow @Final private Registry<StructureTemplatePool> pools;

    @Shadow @Final private int maxDepth;

    @Shadow @Final private ChunkGenerator chunkGenerator;

    @Shadow @Final private StructureTemplateManager structureTemplateManager;

    @Shadow @Final private List<? super PoolElementStructurePiece> pieces;

    @Shadow @Final private RandomSource random;

    @Shadow @Final
    Deque<JigsawPlacement.PieceState> placing;



    private boolean hasWaystone;


    @ModifyArg(method = "tryPlacingChildren(Lnet/minecraft/world/level/levelgen/structure/PoolElementStructurePiece;Lorg/apache/commons/lang3/mutable/MutableObject;IZLnet/minecraft/world/level/LevelHeightAccessor;Lnet/minecraft/world/level/levelgen/RandomState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;getOptional(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;"))
    private ResourceLocation forcTeownCampfirePool(ResourceLocation resourceKey) {
        if (hasWaystone) {
            return resourceKey;
        }

        String poolPath = resourceKey.getPath();

        ResourceLocation alt = Hooks.forceTownCampfirePool(pools,poolPath);

        if (alt != null){
            hasWaystone = true;
            return alt;
        }

        return resourceKey;
    }



   /* @Overwrite
    void tryPlacingChildren(PoolElementStructurePiece pPiece, MutableObject<VoxelShape> p_227266_, int pDepth, boolean p_227268_, LevelHeightAccessor p_227269_,
                            RandomState p_227270_) {
        DebugMethods.tryPlacingChildren(pPiece,p_227266_,pDepth,p_227268_,p_227269_,p_227270_,pools,maxDepth,chunkGenerator,structureTemplateManager,pieces,random,placing);
    }*/
}
