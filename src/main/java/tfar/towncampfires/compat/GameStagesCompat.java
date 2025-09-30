package tfar.towncampfires.compat;

import net.darkhax.gamestages.GameStageHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import tfar.towncampfires.data.quest.Quest;
import tfar.towncampfires.data.quest.QuestRewards;

import java.util.List;
import java.util.UUID;

public class GameStagesCompat {

    public static void onPlayersAcceptQuest(MinecraftServer server, Quest quest, List<UUID> players) {
        for (UUID uuid : players) {
            ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(uuid);
            if (onlinePlayer != null) {
                quest.gameStages().forEach(s -> GameStageHelper.addStage(onlinePlayer, s));
            }
        }
    }

    public static void onPlayersCompleteQuest(ServerPlayer player, QuestRewards rewards) {
        for (String s : rewards.getAddGameStages()) {
            GameStageHelper.addStage(player, s);
        }

        for (String s : rewards.getRemoveGameStages()) {
            GameStageHelper.removeStage(player, s);
        }
    }
}
