package tfar.towncampfires.network.client;


import tfar.towncampfires.network.ModPacket;

public interface S2CModPacket extends ModPacket {
    void handleClient();
}
