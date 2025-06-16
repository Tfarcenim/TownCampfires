package tfar.towncampfires.network;

import net.minecraft.resources.ResourceLocation;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.network.client.S2CCampfireEffectPacket;
import tfar.towncampfires.network.client.S2CQuestInstancePacket;
import tfar.towncampfires.network.client.S2CQuestPacket;
import tfar.towncampfires.network.client.S2CTownCampfirePacket;
import tfar.towncampfires.network.server.C2SSetTownCampfireNamePacket;
import tfar.towncampfires.network.server.C2STownCampfireButtonPacket;
import tfar.towncampfires.network.server.C2STownCampfireStartQuestPacket;

import java.util.Locale;

public class PacketHandler {

    public static void registerPackets() {
        ForgePacketHandler.registerClientPacket(S2CTownCampfirePacket.class,S2CTownCampfirePacket::new);
        ForgePacketHandler.registerClientPacket(S2CCampfireEffectPacket.class,S2CCampfireEffectPacket::new);
        ForgePacketHandler.registerClientPacket(S2CQuestPacket.class, S2CQuestPacket::new);

        ForgePacketHandler.registerClientPacket(S2CQuestInstancePacket.class, S2CQuestInstancePacket::new);

        ForgePacketHandler.registerServerPacket(C2SSetTownCampfireNamePacket.class, C2SSetTownCampfireNamePacket::new);
        ForgePacketHandler.registerServerPacket(C2STownCampfireButtonPacket.class, C2STownCampfireButtonPacket::new);
        ForgePacketHandler.registerServerPacket(C2STownCampfireStartQuestPacket.class, C2STownCampfireStartQuestPacket::new);

    }

    public static ResourceLocation packet(Class<?> clazz) {
        return TownCampfires.id(clazz.getName().toLowerCase(Locale.ROOT));
    }

}
