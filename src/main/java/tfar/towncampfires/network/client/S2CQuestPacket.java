package tfar.towncampfires.network.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import tfar.towncampfires.client.TownCampfiresClient;
import tfar.towncampfires.data.quest.Quest;

import java.util.Map;

public record S2CQuestPacket(Map<ResourceLocation, Quest> effects) implements S2CModPacket {

    public S2CQuestPacket(FriendlyByteBuf buf) {
        this(buf.readMap(FriendlyByteBuf::readResourceLocation, Quest::fromPacket));
    }

    @Override
    public void handleClient() {
        TownCampfiresClient.handle(this);
    }

    @Override
    public void write(FriendlyByteBuf to) {
        to.writeMap(effects, FriendlyByteBuf::writeResourceLocation,(buf, effect) -> effect.toPacket(buf));
    }
}
