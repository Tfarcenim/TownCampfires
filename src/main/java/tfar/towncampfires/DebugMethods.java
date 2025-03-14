package tfar.towncampfires;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DebugMethods {

    //minecraft:village/plains/houses

    public static void tryPlacingChildren(PoolElementStructurePiece pPiece, MutableObject<VoxelShape> free, int pDepth,
                                          boolean useExpansionHack, LevelHeightAccessor level, RandomState randomState,
                                          Registry<StructureTemplatePool> pools,
    final int maxDepth,
    final ChunkGenerator chunkGenerator,
    final StructureTemplateManager structureTemplateManager,
     final List<? super PoolElementStructurePiece> pieces,
     final RandomSource random
    , Deque<JigsawPlacement.PieceState> placing) {


        StructureTemplatePool HOUSES = pools.get(new ResourceLocation("village/plains/houses"));

        StructurePoolElement structurepoolelement = pPiece.getElement();
        BlockPos blockpos = pPiece.getPosition();
        Rotation rotation = pPiece.getRotation();
        StructureTemplatePool.Projection structuretemplatepool$projection = structurepoolelement.getProjection();
        boolean flag = structuretemplatepool$projection == StructureTemplatePool.Projection.RIGID;
        MutableObject<VoxelShape> mutableobject = new MutableObject<>();
        BoundingBox boundingbox = pPiece.getBoundingBox();
        int minY = boundingbox.minY();

        label139:
        for(StructureTemplate.StructureBlockInfo structureBlockInfo : structurepoolelement.getShuffledJigsawBlocks(structureTemplateManager, blockpos, rotation, random)) {
            Direction direction = JigsawBlock.getFrontFacing(structureBlockInfo.state);
            BlockPos pos = structureBlockInfo.pos;
            BlockPos offest = pos.relative(direction);
            int j = pos.getY() - minY;
            int k = -1;
            ResourceLocation pool = new ResourceLocation(structureBlockInfo.nbt.getString("pool"));
            Optional<StructureTemplatePool> optional = pools.getOptional(pool);
            if (optional.isPresent() && (optional.get().size() != 0 || Objects.equals(pool, Pools.EMPTY.location()))) {
                ResourceLocation fallback = optional.get().getFallback();
                Optional<StructureTemplatePool> optional1 = pools.getOptional(fallback);
                if (optional1.isPresent() && (optional1.get().size() != 0 || Objects.equals(fallback, Pools.EMPTY.location()))) {
                    boolean flag1 = boundingbox.isInside(offest);
                    MutableObject<VoxelShape> mutableobject1;
                    if (flag1) {
                        mutableobject1 = mutableobject;
                        if (mutableobject.getValue() == null) {
                            mutableobject.setValue(Shapes.create(AABB.of(boundingbox)));
                        }
                    } else {
                        mutableobject1 = free;
                    }

                    List<StructurePoolElement> list = Lists.newArrayList();
                    if (pDepth != maxDepth) {
                        list.addAll(optional.get().getShuffledTemplates(random));
                    }

                    list.addAll(optional1.get().getShuffledTemplates(random));

                    if (optional.get() == HOUSES) {
                        int i = 0;
                    }

                    for(StructurePoolElement structurepoolelement1 : list) {
                        if (structurepoolelement1 == EmptyPoolElement.INSTANCE) {
                            break;
                        }

                        for(Rotation rotation1 : Rotation.getShuffled(random)) {
                            List<StructureTemplate.StructureBlockInfo> list1 = structurepoolelement1.getShuffledJigsawBlocks(structureTemplateManager, BlockPos.ZERO, rotation1, random);
                            BoundingBox boundingbox1 = structurepoolelement1.getBoundingBox(structureTemplateManager, BlockPos.ZERO, rotation1);
                            int l;
                            if (useExpansionHack && boundingbox1.getYSpan() <= 16) {
                                l = list1.stream().mapToInt((p_210332_) -> {
                                    if (!boundingbox1.isInside(p_210332_.pos.relative(JigsawBlock.getFrontFacing(p_210332_.state)))) {
                                        return 0;
                                    } else {
                                        ResourceLocation resourcelocation2 = new ResourceLocation(p_210332_.nbt.getString("pool"));
                                        Optional<StructureTemplatePool> optional2 = pools.getOptional(resourcelocation2);
                                        Optional<StructureTemplatePool> optional3 = optional2.flatMap((p_210344_) -> pools.getOptional(p_210344_.getFallback()));
                                        int j3 = optional2.map((templatePool) -> templatePool.getMaxSize(structureTemplateManager)).orElse(0);
                                        int k3 = optional3.map((templatePool) -> templatePool.getMaxSize(structureTemplateManager)).orElse(0);
                                        return Math.max(j3, k3);
                                    }
                                }).max().orElse(0);
                            } else {
                                l = 0;
                            }

                            for(StructureTemplate.StructureBlockInfo structureBlockInfo1 : list1) {
                                if (JigsawBlock.canAttach(structureBlockInfo, structureBlockInfo1)) {
                                    BlockPos blockpos3 = structureBlockInfo1.pos;
                                    BlockPos blockpos4 = offest.subtract(blockpos3);
                                    BoundingBox boundingbox2 = structurepoolelement1.getBoundingBox(structureTemplateManager, blockpos4, rotation1);
                                    int i1 = boundingbox2.minY();
                                    StructureTemplatePool.Projection structuretemplatepool$projection1 = structurepoolelement1.getProjection();
                                    boolean flag2 = structuretemplatepool$projection1 == StructureTemplatePool.Projection.RIGID;
                                    int j1 = blockpos3.getY();
                                    int k1 = j - j1 + JigsawBlock.getFrontFacing(structureBlockInfo.state).getStepY();
                                    int l1;
                                    if (flag && flag2) {
                                        l1 = minY + k1;
                                    } else {
                                        if (k == -1) {
                                            k = chunkGenerator.getFirstFreeHeight(pos.getX(), pos.getZ(), Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
                                        }

                                        l1 = k - j1;
                                    }

                                    int i2 = l1 - i1;
                                    BoundingBox boundingbox3 = boundingbox2.moved(0, i2, 0);
                                    BlockPos blockpos5 = blockpos4.offset(0, i2, 0);
                                    if (l > 0) {
                                        int j2 = Math.max(l + 1, boundingbox3.maxY() - boundingbox3.minY());
                                        boundingbox3.encapsulate(new BlockPos(boundingbox3.minX(), boundingbox3.minY() + j2, boundingbox3.minZ()));
                                    }

                                    if (!Shapes.joinIsNotEmpty(mutableobject1.getValue(), Shapes.create(AABB.of(boundingbox3).deflate(0.25D)), BooleanOp.ONLY_SECOND)) {
                                        mutableobject1.setValue(Shapes.joinUnoptimized(mutableobject1.getValue(), Shapes.create(AABB.of(boundingbox3)), BooleanOp.ONLY_FIRST));
                                        int i3 = pPiece.getGroundLevelDelta();
                                        int k2;
                                        if (flag2) {
                                            k2 = i3 - k1;
                                        } else {
                                            k2 = structurepoolelement1.getGroundLevelDelta();
                                        }

                                        PoolElementStructurePiece poolelementstructurepiece = new PoolElementStructurePiece(structureTemplateManager, structurepoolelement1, blockpos5, k2, rotation1, boundingbox3);
                                        int l2;
                                        if (flag) {
                                            l2 = minY + j;
                                        } else if (flag2) {
                                            l2 = l1 + j1;
                                        } else {
                                            if (k == -1) {
                                                k = chunkGenerator.getFirstFreeHeight(pos.getX(), pos.getZ(), Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
                                            }

                                            l2 = k + k1 / 2;
                                        }

                                        pPiece.addJunction(new JigsawJunction(offest.getX(), l2 - j + i3, offest.getZ(), k1, structuretemplatepool$projection1));
                                        poolelementstructurepiece.addJunction(new JigsawJunction(pos.getX(), l2 - j1 + k2, pos.getZ(), -k1, structuretemplatepool$projection));
                                        pieces.add(poolelementstructurepiece);
                                        if (pDepth + 1 <= maxDepth) {
                                            placing.addLast(new JigsawPlacement.PieceState(poolelementstructurepiece, mutableobject1, pDepth + 1));
                                        }
                                        continue label139;
                                    }
                                }
                            }
                        }
                    }
                } else {
              //      JigsawPlacement.LOGGER.warn("Empty or non-existent fallback pool: {}", resourcelocation1);
                }
            } else {
                TownCampfires.LOGGER.warn("Empty or non-existent pool: {}", pool);
            }
        }

    }
}
