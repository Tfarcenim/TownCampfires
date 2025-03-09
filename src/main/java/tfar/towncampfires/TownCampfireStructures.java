package tfar.towncampfires;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import tfar.towncampfires.init.ModBlocks;

import java.util.ArrayList;
import java.util.List;

public class TownCampfireStructures {
    private static final ResourceKey<StructureProcessorList> EMPTY_PROCESSOR_LIST_KEY = ResourceKey.create(
            Registry.PROCESSOR_LIST_REGISTRY, new ResourceLocation("empty"));

    private static void addBuildingToPool(Registry<StructureTemplatePool> templatePoolRegistry,
                                          ResourceLocation poolRL,
                                          String nbtPieceRL,
                                          Holder<StructureProcessorList> processors,
                                          int weight) {

        StructureTemplatePool pool = templatePoolRegistry.get(poolRL);
        if (pool == null) {
            return;
        }

        SinglePoolElement piece = SinglePoolElement.legacy(nbtPieceRL, processors).apply(StructureTemplatePool.Projection.RIGID);

        for (int i = 0; i < weight; i++) {
            pool.templates.add(piece);
        }

        List<Pair<StructurePoolElement, Integer>> listOfPieceEntries = new ArrayList<>(pool.rawTemplates);
        listOfPieceEntries.add(new Pair<>(piece, weight));
        pool.rawTemplates = listOfPieceEntries;
    }

    public static void setup(RegistryAccess registryAccess) {
        TownCampfires.LOGGER.info("Injecting Carpenter Village Houses");

        Registry<StructureTemplatePool> templatePoolRegistry = registryAccess.registry(Registry.TEMPLATE_POOL_REGISTRY).orElseThrow();
        Registry<StructureProcessorList> processorListRegistry = registryAccess.registry(Registry.PROCESSOR_LIST_REGISTRY).orElseThrow();

        String redCampfire = Registry.BLOCK.getKey(ModBlocks.RED_TOWN_CAMPFIRE).toString();

        int weight = 25;

        addVillageHouse(templatePoolRegistry, processorListRegistry,
                "plains", redCampfire, weight);


        addVillageHouse(templatePoolRegistry, processorListRegistry,
                "snowy", redCampfire, weight);

        addVillageHouse(templatePoolRegistry, processorListRegistry,
                "savanna", redCampfire, weight);

        addVillageHouse(templatePoolRegistry, processorListRegistry,
                "taiga", redCampfire, weight);


        addVillageHouse(templatePoolRegistry, processorListRegistry,
                "desert", redCampfire, weight);
    }

    private static void addVillageHouse(Registry<StructureTemplatePool> templatePoolRegistry,
                                        Registry<StructureProcessorList> processorListRegistry,
                                        String villageName, String pieceName,
                                        int weight) {

        Holder<StructureProcessorList> normalProcessor =
                processorListRegistry.getHolderOrThrow(EMPTY_PROCESSOR_LIST_KEY);

        Holder<StructureProcessorList> zombieProcessor = processorListRegistry.getHolderOrThrow(ResourceKey.create(
                Registry.PROCESSOR_LIST_REGISTRY, new ResourceLocation("zombie_" + villageName)
        ));

        addBuildingToPool(templatePoolRegistry, new ResourceLocation("village/" + villageName + "/houses"),
                pieceName, normalProcessor, weight);

        addBuildingToPool(templatePoolRegistry, new ResourceLocation("village/" + villageName + "/zombie/houses"),
                pieceName, zombieProcessor, weight);
    }

}
