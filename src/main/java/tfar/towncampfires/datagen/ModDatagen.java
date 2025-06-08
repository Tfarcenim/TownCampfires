package tfar.towncampfires.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.resources.RegistryOps;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

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

        generator.addProvider(server,new CampfireEffectProvider(generator));
        generator.addProvider(server,new QuestProvider(generator));
    }



}
