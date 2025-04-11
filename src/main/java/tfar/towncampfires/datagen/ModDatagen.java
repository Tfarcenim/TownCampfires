package tfar.towncampfires.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.init.ModBlocks;
import tfar.towncampfires.init.ModPOIs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModDatagen {
    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper helper = event.getExistingFileHelper();
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.builtinCopy());
        boolean client = event.includeClient();
        boolean server = event.includeServer();
        generator.addProvider(client,new ModBlockstateProvider(generator,helper));
        generator.addProvider(client,new ModLangProvider(generator));
        BlockTagsProvider blockTagsProvider = new ModBlockTagsProvider(generator,helper);
        generator.addProvider(server,new ModItemTagProvider(generator,blockTagsProvider,helper));
        generator.addProvider(client,new ModItemModelProvider(generator,helper));
    }



}
