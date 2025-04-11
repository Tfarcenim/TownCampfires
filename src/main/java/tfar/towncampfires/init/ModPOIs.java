package tfar.towncampfires.init;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;
import tfar.towncampfires.TownCampfires;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModPOIs {
    //public static final ResourceKey<PoiType> TOWN_CAMPFIRE = createKey("town_campfire");
    public static final PoiType TOWN_CAMPFIRE = new PoiType(generatePoiTypes(),0,1);

    static Set<BlockState> generatePoiTypes() {
        Map<ResourceLocation,PoiType> typeMap = new HashMap<>();
        Set<BlockState> campfireStates = new HashSet<>();
        campfireStates.addAll(ModBlocks.GRAY_TOWN_CAMPFIRE.getStateDefinition().getPossibleStates());
        campfireStates.addAll(ModBlocks.GREEN_TOWN_CAMPFIRE.getStateDefinition().getPossibleStates());
        campfireStates.addAll(ModBlocks.LIGHT_BLUE_TOWN_CAMPFIRE.getStateDefinition().getPossibleStates());
        campfireStates.addAll(ModBlocks.ORANGE_TOWN_CAMPFIRE.getStateDefinition().getPossibleStates());
        campfireStates.addAll(ModBlocks.RED_TOWN_CAMPFIRE.getStateDefinition().getPossibleStates());
        //typeMap.put(ModPOIs.TOWN_CAMPFIRE.location(), new PoiType(campfireStates, 0, 1));
        return campfireStates;
    }

    private static ResourceKey<PoiType> createKey(String pName) {
        return ResourceKey.create(Registry.POINT_OF_INTEREST_TYPE_REGISTRY, TownCampfires.id(pName));
    }
}
