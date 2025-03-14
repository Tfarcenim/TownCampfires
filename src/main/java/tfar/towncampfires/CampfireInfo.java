package tfar.towncampfires;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record CampfireInfo(BlockPos location) {
    public static final Codec<CampfireInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(BlockPos.CODEC.fieldOf("location")
            .forGetter(CampfireInfo::location)).apply(instance,CampfireInfo::new));
}
