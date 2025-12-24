package tfar.towncampfires;


import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.Date;

public class QuestLogger {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void log(MinecraftServer server,Component component) {
       // ForgePacketHandler.sendToAll(new S2CAddQuestLogPacket(component));
    }

    public static void init() {
    }

    //[player name] has completed quest [quest name] at [time]
    public static MutableComponent questComplete(Component playerName,Component questName) {
        Date date = Date.from(Instant.now());
        Component dateC = Component.literal(date.toString());
        return Component.translatable("log.towncampfires.quest_complete",playerName,questName,dateC);
    }

    //[player name] has completed quest [quest name] at [time]
    public static MutableComponent questFailed(Component playerName,Component questName) {
        Date date = Date.from(Instant.now());
        Component dateC = Component.literal(date.toString());
        return Component.translatable("log.towncampfires.quest_failed",playerName,questName,dateC);
    }
}