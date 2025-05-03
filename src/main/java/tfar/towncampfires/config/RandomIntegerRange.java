package tfar.towncampfires.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

public record RandomIntegerRange(int min, int max, double increaseChance) {

    public static final Codec<RandomIntegerRange> CODEC = RecordCodecBuilder.create(integerRangeInstance -> integerRangeInstance.group(
                    Codec.INT.fieldOf("min").forGetter(RandomIntegerRange::min),
                    Codec.INT.fieldOf("max").forGetter(RandomIntegerRange::max),
                    Codec.DOUBLE.fieldOf("increase_chance").forGetter(RandomIntegerRange::increaseChance)
            ).apply(integerRangeInstance, RandomIntegerRange::new)
    );

    public int roll(RandomSource random) {
        int i = min;
        while (i < max && random.nextDouble() < increaseChance) {
            i++;
        }
        return i;
    }
}
