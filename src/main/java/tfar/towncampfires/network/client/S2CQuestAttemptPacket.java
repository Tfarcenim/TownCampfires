package tfar.towncampfires.network.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import tfar.towncampfires.client.TownCampfiresClient;

import java.util.Map;

public record S2CQuestAttemptPacket(Map<ResourceLocation, Integer> attempts) implements S2CModPacket {

    public S2CQuestAttemptPacket(FriendlyByteBuf buf) {
        this(buf.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readInt));
    }

    @Override
    public void handleClient() {
        TownCampfiresClient.handle(this);
    }

    @Override
    public void write(FriendlyByteBuf to) {
        to.writeMap(attempts, FriendlyByteBuf::writeResourceLocation,FriendlyByteBuf::writeInt);
    }
}
