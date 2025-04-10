package tfar.towncampfires.network.client;

import net.minecraft.network.FriendlyByteBuf;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.client.TownCampfiresClient;

public record S2CTownCampfirePacket(TownCampfire townCampfire) implements S2CModPacket {

    public S2CTownCampfirePacket(FriendlyByteBuf buf) {
        this(TownCampfire.fromPacket(buf));
    }

    @Override
    public void handleClient() {
        TownCampfiresClient.handleSync(townCampfire);
    }

    @Override
    public void write(FriendlyByteBuf to) {
        townCampfire.toPacket(to);
    }
}
