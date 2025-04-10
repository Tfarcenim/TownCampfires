package tfar.towncampfires;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import tfar.towncampfires.utils.MiscCodecs;

import java.util.Objects;

public final class TownCampfire {
    public static final Codec<TownCampfire> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    BlockPos.CODEC.fieldOf("location").forGetter(TownCampfire::location),
                    MiscCodecs.COMPONENT_CODEC.fieldOf("name").forGetter(TownCampfire::name)


            ).apply(instance, TownCampfire::new));
    private final BlockPos location;
    private Component name;

    public TownCampfire(BlockPos location, Component name) {
        this.location = location;
        this.name = name;
    }

    public void toPacket(FriendlyByteBuf buf) {
        buf.writeBlockPos(location);
        buf.writeComponent(name);
    }

    public static TownCampfire fromPacket(FriendlyByteBuf buf) {
        BlockPos location = buf.readBlockPos();
        Component name = buf.readComponent();
        return new TownCampfire(location, name);
    }

    public BlockPos location() {
        return location;
    }

    public Component name() {
        return name;
    }

    public void setName(Component name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TownCampfire) obj;
        return Objects.equals(this.location, that.location) &&
                Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, name);
    }

    @Override
    public String toString() {
        return "TownCampfire[" +
                "location=" + location + ", " +
                "name=" + name + ']';
    }


}
