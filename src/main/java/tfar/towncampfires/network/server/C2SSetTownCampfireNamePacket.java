package tfar.towncampfires.network.server;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import tfar.towncampfires.CampfireLevelData;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.TownCampfires;

public class C2SSetTownCampfireNamePacket implements C2SModPacket {

    final String name;
    final BlockPos pos;

    public C2SSetTownCampfireNamePacket(FriendlyByteBuf buf) {
        name = buf.readUtf();
        pos = buf.readBlockPos();
    }

    public C2SSetTownCampfireNamePacket(String name,BlockPos pos) {
        this.name = name;
        this.pos = pos;
    }

    @Override
    public void handleServer(ServerPlayer player) {
        CampfireLevelData campfireLevelData = CampfireLevelData.getOrCreate(player.getLevel());
        TownCampfire townCampfire = campfireLevelData.byLocation(pos);
        if (townCampfire != null) {
            townCampfire.setName(Component.literal(name));
            campfireLevelData.setDirty();
        } else {
            TownCampfires.LOGGER.warn("Player {} attempted to rename nonexistent campfire at {}",player,pos);
        }
    }

    @Override
    public void write(FriendlyByteBuf to) {
        to.writeUtf(name);
        to.writeBlockPos(pos);
    }

}
