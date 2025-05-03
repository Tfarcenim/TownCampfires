package tfar.towncampfires.network.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import tfar.towncampfires.CampfireEffect;
import tfar.towncampfires.client.TownCampfiresClient;

import java.util.Map;

public record S2CCampfireEffectPacket(Map<ResourceLocation, CampfireEffect> effects) implements S2CModPacket {

    public S2CCampfireEffectPacket(FriendlyByteBuf buf) {
        this(buf.readMap(FriendlyByteBuf::readResourceLocation, CampfireEffect::fromPacket));
    }

    @Override
    public void handleClient() {
        TownCampfiresClient.campfireEffectLoader.setFromServer(effects);
    }

    @Override
    public void write(FriendlyByteBuf to) {
        to.writeMap(effects, FriendlyByteBuf::writeResourceLocation,(buf, effect) -> effect.toPacket(buf));
    }
}
