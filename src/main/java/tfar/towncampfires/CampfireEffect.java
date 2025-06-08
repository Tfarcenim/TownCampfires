package tfar.towncampfires;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.biome.Biome;
import tfar.towncampfires.config.IntegerRange;
import tfar.towncampfires.utils.MiscCodecs;

import java.util.List;

public record CampfireEffect(Component name, List<Component> desc, MobEffectCategory category, TagKey<Biome> requiredBiomes,
                             TagKey<Biome> requiredNearbyBiomes, IntegerRange levelRange, int weight, MobEffectInstance effect, boolean hidden) {

    public static final Codec<CampfireEffect> CODEC = RecordCodecBuilder.create(campfireEffectInstance -> campfireEffectInstance
            .group(
                    MiscCodecs.COMPONENT_CODEC.fieldOf("name").forGetter(CampfireEffect::name),
                    MiscCodecs.COMPONENT_CODEC.listOf().fieldOf("desc").forGetter(CampfireEffect::desc),
                    MiscCodecs.MOB_EFFECT_CATEGORY_CODEC.fieldOf("category").forGetter(CampfireEffect::category),
                    TagKey.codec(Registry.BIOME_REGISTRY).fieldOf("required_biomes").forGetter(CampfireEffect::requiredBiomes),
                    TagKey.codec(Registry.BIOME_REGISTRY).fieldOf("required_nearby_biomes").forGetter(CampfireEffect::requiredNearbyBiomes),
                    IntegerRange.CODEC.fieldOf("level_range").forGetter(CampfireEffect::levelRange),
                    ExtraCodecs.POSITIVE_INT.fieldOf("weight").forGetter(CampfireEffect::weight),
                    MiscCodecs.MOB_EFFECT_INSTANCE_CODEC.fieldOf("effect").forGetter(CampfireEffect::effect),
                    Codec.BOOL.fieldOf("hidden").forGetter(CampfireEffect::hidden)
                    ).apply(campfireEffectInstance,CampfireEffect::new)
    );

    public void toPacket(FriendlyByteBuf buf) {
        buf.writeComponent(name);
        buf.writeCollection(desc, FriendlyByteBuf::writeComponent);
        buf.writeEnum(category);
        writeTag(buf,requiredBiomes);
        writeTag(buf,requiredNearbyBiomes);
        levelRange.toPacket(buf);
        buf.writeInt(weight);
        MiscCodecs.write(effect,buf);
        buf.writeBoolean(hidden);
    }

    public static CampfireEffect fromPacket(FriendlyByteBuf buf) {
        return new CampfireEffect(buf.readComponent(),buf.readList(FriendlyByteBuf::readComponent),buf.readEnum(MobEffectCategory.class),
                readTag(buf),readTag(buf),IntegerRange.fromPacket(buf),buf.readInt(),MiscCodecs.from(buf),buf.readBoolean());
    }

    void writeTag(FriendlyByteBuf buf,TagKey<Biome> tagKey) {
        buf.writeResourceKey(ResourceKey.create(Registry.BIOME_REGISTRY,tagKey.location()));
    }

    static TagKey<Biome> readTag(FriendlyByteBuf buf) {
        ResourceKey<Biome> biomeResourceKey = buf.readResourceKey(Registry.BIOME_REGISTRY);
        return TagKey.create(Registry.BIOME_REGISTRY,biomeResourceKey.location());
    }

}
