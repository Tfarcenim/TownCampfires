package tfar.towncampfires.network.server;

import net.minecraft.server.level.ServerPlayer;
import tfar.towncampfires.network.ModPacket;

public interface C2SModPacket extends ModPacket {

    void handleServer(ServerPlayer player);

}
