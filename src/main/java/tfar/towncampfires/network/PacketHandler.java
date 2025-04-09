package tfar.towncampfires.network;

import net.minecraft.resources.ResourceLocation;
import tfar.towncampfires.TownCampfires;

import java.util.Locale;

public class PacketHandler {

    public static void registerPackets() {

    }

    public static ResourceLocation packet(Class<?> clazz) {
        return TownCampfires.id(clazz.getName().toLowerCase(Locale.ROOT));
    }

}
