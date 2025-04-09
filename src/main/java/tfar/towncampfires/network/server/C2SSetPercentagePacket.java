package tfar.towncampfires.network.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class C2SSetPercentagePacket implements C2SModPacket {

    public final double percent;

    public C2SSetPercentagePacket(FriendlyByteBuf buf) {
        percent = buf.readDouble();
    }

    public C2SSetPercentagePacket(double percent) {
        this.percent = percent;
    }

    @Override
    public void handleServer(ServerPlayer player) {
    }

    @Override
    public void write(FriendlyByteBuf to) {
        to.writeDouble(percent);
    }

}
