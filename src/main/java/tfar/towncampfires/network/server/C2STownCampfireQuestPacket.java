package tfar.towncampfires.network.server;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import tfar.towncampfires.CampfireLevelData;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.TownCampfires;

public class C2STownCampfireQuestPacket implements C2SModPacket {

    final ResourceLocation questID;
    final BlockPos pos;
    Type type;

    public C2STownCampfireQuestPacket(FriendlyByteBuf buf) {
        questID = buf.readResourceLocation();
        pos = buf.readBlockPos();
        type = buf.readEnum(Type.class);
    }

    public C2STownCampfireQuestPacket(ResourceLocation questID, BlockPos pos,Type type) {
        this.questID = questID;
        this.pos = pos;
        this.type = type;
    }

    @Override
    public void handleServer(ServerPlayer player) {
        CampfireLevelData campfireLevelData = CampfireLevelData.getOrCreate(player.getLevel());
        TownCampfire townCampfire = campfireLevelData.byLocation(pos);
        if (townCampfire != null) {
            switch (type) {
                case START -> campfireLevelData.startQuest(player,questID,townCampfire);
                case FINISH -> campfireLevelData.finishQuest(player,questID);
            }

        } else {
            TownCampfires.LOGGER.warn("Player {} attempted to access nonexistent campfire at {}",player,pos);
        }
    }

    @Override
    public void write(FriendlyByteBuf to) {
        to.writeResourceLocation(questID);
        to.writeBlockPos(pos);
        to.writeEnum(type);
    }

    public enum Type {
        START, FINISH;
    }
}
