package tfar.towncampfires.network.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import tfar.towncampfires.client.TownCampfiresClient;

public record S2CAddQuestLogPacket(Component message) implements S2CModPacket{

    public S2CAddQuestLogPacket(FriendlyByteBuf buf) {
        this(buf.readComponent());
    }

    @Override
    public void handleClient() {
        TownCampfiresClient.addLog(message);
    }

    @Override
    public void write(FriendlyByteBuf to) {
        to.writeComponent(message);
    }
}
