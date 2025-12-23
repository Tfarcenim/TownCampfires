package tfar.towncampfires.network.client;

import net.minecraft.network.FriendlyByteBuf;
import tfar.towncampfires.client.TownCampfiresClient;
import tfar.towncampfires.data.QuestLogEntry;

import java.util.List;

public record S2CFillQuestLogPacket(List<QuestLogEntry> messages) implements S2CModPacket{

    public S2CFillQuestLogPacket(FriendlyByteBuf buf) {
        this(buf.readList(QuestLogEntry::fromPacket));
    }

    @Override
    public void handleClient() {
        TownCampfiresClient.setLogs(messages);
    }

    @Override
    public void write(FriendlyByteBuf to) {
        to.writeCollection(messages,(buf, questLogEntry) -> questLogEntry.toPacket(buf));
    }
}
