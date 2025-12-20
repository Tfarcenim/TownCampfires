package tfar.towncampfires.network;

import net.minecraft.resources.ResourceLocation;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.network.client.*;
import tfar.towncampfires.network.server.C2SSetTownCampfireNamePacket;
import tfar.towncampfires.network.server.C2STeleportPacket;
import tfar.towncampfires.network.server.C2STownCampfireButtonPacket;
import tfar.towncampfires.network.server.C2STownCampfireQuestPacket;

import java.util.Locale;

public class PacketHandler {

    public static void registerPackets() {
        ForgePacketHandler.registerClientPacket(S2CTownCampfirePacket.class,S2CTownCampfirePacket::new);
        ForgePacketHandler.registerClientPacket(S2CCampfireEffectPacket.class,S2CCampfireEffectPacket::new);
        ForgePacketHandler.registerClientPacket(S2CQuestPacket.class, S2CQuestPacket::new);
        ForgePacketHandler.registerClientPacket(S2CQuestAttemptPacket.class, S2CQuestAttemptPacket::new);

        ForgePacketHandler.registerClientPacket(S2CAddQuestLogPacket.class, S2CAddQuestLogPacket::new);
        ForgePacketHandler.registerClientPacket(S2CFillQuestLogPacket.class, S2CFillQuestLogPacket::new);
        ForgePacketHandler.registerClientPacket(S2CClearQuestLogPacket.class, S2CClearQuestLogPacket::fromPacket);

        ForgePacketHandler.registerClientPacket(S2CQuestInstancePacket.class, S2CQuestInstancePacket::new);
        ForgePacketHandler.registerClientPacket(S2CTeleportsPacket.class, S2CTeleportsPacket::fromPacket);

        ForgePacketHandler.registerServerPacket(C2SSetTownCampfireNamePacket.class, C2SSetTownCampfireNamePacket::new);
        ForgePacketHandler.registerServerPacket(C2STownCampfireButtonPacket.class, C2STownCampfireButtonPacket::new);
        ForgePacketHandler.registerServerPacket(C2STownCampfireQuestPacket.class, C2STownCampfireQuestPacket::new);
        ForgePacketHandler.registerServerPacket(C2STeleportPacket.class, C2STeleportPacket::fromPacket);

    }

    public static ResourceLocation packet(Class<?> clazz) {
        return TownCampfires.id(clazz.getName().toLowerCase(Locale.ROOT));
    }

}
