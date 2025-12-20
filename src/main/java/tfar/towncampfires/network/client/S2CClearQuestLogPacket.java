package tfar.towncampfires.network.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import tfar.towncampfires.client.TownCampfiresClient;
import tfar.towncampfires.network.ForgePacketHandler;

public record S2CClearQuestLogPacket() implements S2CModPacket{

    public static S2CClearQuestLogPacket fromPacket(FriendlyByteBuf buf) {
        return new S2CClearQuestLogPacket();
    }

    @Override
    public void handleClient() {
        TownCampfiresClient.handle(this);
    }

    public void send(MinecraftServer server) {
        ForgePacketHandler.sendToAll(this);
    }

    @Override
    public void write(FriendlyByteBuf to) {
    }
}
