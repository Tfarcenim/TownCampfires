package tfar.towncampfires.data.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import tfar.towncampfires.config.IntegerRange;
import tfar.towncampfires.utils.MiscCodecs;

public record QuestAppearanceConditions(TagKey<Biome> biomeWhitelist, boolean isWhiteList, TagKey<Biome> nearbyBiomes
, IntegerRange levelRange,int weight,IntegerRange spawnDistance) {

    public static final Codec<QuestAppearanceConditions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    TagKey.codec(Registry.BIOME_REGISTRY).fieldOf("biome_whitelist").forGetter(QuestAppearanceConditions::biomeWhitelist),
                    Codec.BOOL.fieldOf("whitelist").forGetter(QuestAppearanceConditions::isWhiteList),
                    TagKey.codec(Registry.BIOME_REGISTRY).fieldOf("nearby_biomes").forGetter(QuestAppearanceConditions::nearbyBiomes),
                    IntegerRange.CODEC.fieldOf("level_range").forGetter(QuestAppearanceConditions::levelRange),
                    Codec.INT.fieldOf("weight").forGetter(QuestAppearanceConditions::weight),
                    IntegerRange.CODEC.fieldOf("spawn_distance").forGetter(QuestAppearanceConditions::spawnDistance)
                    ).apply(instance,QuestAppearanceConditions::new)
    );

    public void toPacket(FriendlyByteBuf buf) {
        MiscCodecs.writeTag(buf,biomeWhitelist);
        buf.writeBoolean(isWhiteList);
        MiscCodecs.writeTag(buf,nearbyBiomes);
        levelRange.toPacket(buf);
        buf.writeInt(weight);
        spawnDistance.toPacket(buf);
    }

    public static QuestAppearanceConditions fromPacket(FriendlyByteBuf buf) {
        return new QuestAppearanceConditions(MiscCodecs.readTag(buf),buf.readBoolean(),MiscCodecs.readTag(buf),
                IntegerRange.fromPacket(buf),buf.readInt(),IntegerRange.fromPacket(buf));
    }
}
