package tfar.towncampfires;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(TownCampfires.MODID)
                .then(Commands.literal("info")
                        .executes(ModCommands::printInfo)
                )
                .then(Commands.literal("refresh")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ModCommands::manualRefresh)
                        )
                )
        );
    }

    private static int manualRefresh(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos blockPos = BlockPosArgument.getLoadedBlockPos(context,"pos");

        BlockEntity be = level.getBlockEntity(blockPos);

        if (be instanceof TownCampfireBlockEntity tcbe) {
            TownCampfire townCampfire = tcbe.townCampfire;
            townCampfire.refresh(level);
            source.sendSuccess(Component.literal("Refreshed campfire"),true);
            return 1;
        } else {
            source.sendFailure(Component.literal("No campfire located"));
            return 0;
        }

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
