package tfar.towncampfires.network;

import net.minecraft.resources.ResourceLocation;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.network.client.S2CTownCampfirePacket;
import tfar.towncampfires.network.server.C2SSetTownCampfireNamePacket;

import java.util.Locale;

public class PacketHandler {

    public static void registerPackets() {
        ForgePacketHandler.registerClientPacket(S2CTownCampfirePacket.class,S2CTownCampfirePacket::new);
        ForgePacketHandler.registerServerPacket(C2SSetTownCampfireNamePacket.class, C2SSetTownCampfireNamePacket::new);
    }

    public static ResourceLocation packet(Class<?> clazz) {
        return TownCampfires.id(clazz.getName().toLowerCase(Locale.ROOT));
    }

}
