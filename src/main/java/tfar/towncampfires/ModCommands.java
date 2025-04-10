package tfar.towncampfires;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(TownCampfires.MODID)
                .then(Commands.literal("info")
                        .executes(ModCommands::printInfo)
                )
        );
    }

    private static int printInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        CampfireLevelData campfireLevelData = CampfireLevelData.getOrCreate(server.overworld());//todo other worlds?

        List<TownCampfire> campfiresByIndex = campfireLevelData.getCampfiresByIndex();

        for (int i = 0; i < campfiresByIndex.size();i++) {
            TownCampfire townCampfire = campfiresByIndex.get(i);
            MutableComponent component = Component.literal("Campfire "+i+": "+ townCampfire.location());
            source.sendSuccess(component,false);
        }
        return 1;
    }
}
