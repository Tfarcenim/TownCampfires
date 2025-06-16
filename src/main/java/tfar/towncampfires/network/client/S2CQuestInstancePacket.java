package tfar.towncampfires.network.client;

import net.minecraft.network.FriendlyByteBuf;
import tfar.towncampfires.client.TownCampfiresClient;
import tfar.towncampfires.data.quest.QuestInstance;

import java.util.List;

public record S2CQuestInstancePacket(List<QuestInstance> questInstanceList) implements S2CModPacket {

    public S2CQuestInstancePacket(FriendlyByteBuf buf) {
        this(buf.readList(QuestInstance::fromPacket));
    }

    @Override
    public void handleClient() {
        TownCampfiresClient.setQuests(questInstanceList);
    }

    @Override
    public void write(FriendlyByteBuf to) {
        to.writeCollection(questInstanceList,(buf, questInstance) -> questInstance.toPacket(buf));
    }
}
