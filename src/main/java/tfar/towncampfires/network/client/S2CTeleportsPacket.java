package tfar.towncampfires.network.client;

import net.minecraft.network.FriendlyByteBuf;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.client.TownCampfiresClient;

import java.util.List;

public record S2CTeleportsPacket(List<TownCampfire> campfires) implements S2CModPacket{
    @Override
    public void handleClient() {
        TownCampfiresClient.handleSyncTeleports(campfires);
    }

    public static S2CTeleportsPacket fromPacket(FriendlyByteBuf buf){
        return new S2CTeleportsPacket(buf.readList(TownCampfire::fromPacket));
    }

    @Override
    public void write(FriendlyByteBuf to) {
        to.writeCollection(campfires,(buf, campfire) -> campfire.toPacket(buf));
    }
}
