package tfar.towncampfires.mixin.debug;

import net.minecraft.core.Registry;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import tfar.towncampfires.DebugMethods;

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

    @Overwrite
    void tryPlacingChildren(PoolElementStructurePiece pPiece, MutableObject<VoxelShape> p_227266_, int pDepth, boolean p_227268_, LevelHeightAccessor p_227269_,
                            RandomState p_227270_) {
        DebugMethods.tryPlacingChildren(pPiece,p_227266_,pDepth,p_227268_,p_227269_,p_227270_,pools,maxDepth,chunkGenerator,structureTemplateManager,pieces,random,placing);
    }
    }
