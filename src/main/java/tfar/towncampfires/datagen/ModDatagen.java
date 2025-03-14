package tfar.towncampfires.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

public class ModDatagen {
    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper helper = event.getExistingFileHelper();
        boolean client = event.includeClient();
        generator.addProvider(client,new ModBlockstateProvider(generator,helper));
        generator.addProvider(client,new ModLangProvider(generator));
    }
}
