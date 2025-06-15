package tfar.towncampfires.network.server;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import tfar.towncampfires.CampfireLevelData;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.TownCampfires;

public class C2STownCampfireStartQuestPacket implements C2SModPacket {

    final ResourceLocation questID;
    final BlockPos pos;

    public C2STownCampfireStartQuestPacket(FriendlyByteBuf buf) {
        questID = buf.readResourceLocation();
        pos = buf.readBlockPos();
    }

    public C2STownCampfireStartQuestPacket(ResourceLocation questID, BlockPos pos) {
        this.questID = questID;
        this.pos = pos;
    }

    @Override
    public void handleServer(ServerPlayer player) {
        CampfireLevelData campfireLevelData = CampfireLevelData.getOrCreate(player.getLevel());
        TownCampfire townCampfire = campfireLevelData.byLocation(pos);
        if (townCampfire != null) {
            townCampfire.startQuest(player,questID);
        } else {
            TownCampfires.LOGGER.warn("Player {} attempted to access nonexistent campfire at {}",player,pos);
        }
    }

    @Override
    public void write(FriendlyByteBuf to) {
        to.writeResourceLocation(questID);
        to.writeBlockPos(pos);
    }


}
