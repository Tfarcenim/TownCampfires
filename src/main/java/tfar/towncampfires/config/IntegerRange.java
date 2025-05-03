package tfar.towncampfires.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

public record IntegerRange(int min, int max) {
    public static final Codec<IntegerRange> CODEC = RecordCodecBuilder.create(integerRangeInstance -> integerRangeInstance.group(
            Codec.INT.fieldOf("min").forGetter(IntegerRange::min),
            Codec.INT.fieldOf("max").forGetter(IntegerRange::max)
            ).apply(integerRangeInstance,IntegerRange::new)
    );
    public static IntegerRange inclusive(int min,int max) {
        return new IntegerRange(min,max);
    }

    public void toPacket(FriendlyByteBuf buf) {
        buf.writeInt(min);
        buf.writeInt(max);
    }

    public static IntegerRange fromPacket(FriendlyByteBuf buf) {
        return inclusive(buf.readInt(),buf.readInt());
    }

}
