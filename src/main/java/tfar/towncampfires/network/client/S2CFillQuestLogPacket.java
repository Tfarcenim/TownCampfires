package tfar.towncampfires.network.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import tfar.towncampfires.client.TownCampfiresClient;

import java.util.List;

public record S2CFillQuestLogPacket(List<Component> messages) implements S2CModPacket{

    public S2CFillQuestLogPacket(FriendlyByteBuf buf) {
        this(buf.readList(FriendlyByteBuf::readComponent));
    }

    @Override
    public void handleClient() {
        TownCampfiresClient.setLogs(messages);
    }

    @Override
    public void write(FriendlyByteBuf to) {
        to.writeCollection(messages, FriendlyByteBuf::writeComponent);
    }
}
