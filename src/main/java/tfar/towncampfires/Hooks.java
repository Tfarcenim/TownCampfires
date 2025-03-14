package tfar.towncampfires;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class Hooks {
    public static ResourceLocation forceWaystonePool(Registry<StructureTemplatePool> pools, String poolPath) {
        if (poolPath.endsWith("/houses")) {
            ResourceLocation waystonePoolName = TownCampfires.id(poolPath.replace("/houses", "/campfires"));
            if (pools.containsKey(waystonePoolName)) {
                return waystonePoolName;
            }
        }
        return null;
    }
}
