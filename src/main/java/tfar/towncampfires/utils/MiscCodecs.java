package tfar.towncampfires.utils;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.biome.Biome;

import java.util.Optional;

public class MiscCodecs {
    public static final Codec<Component> COMPONENT_CODEC = Codec.STRING.xmap(Component.Serializer::fromJson, Component.Serializer::toJson);
    public static final Codec<MobEffectCategory> MOB_EFFECT_CATEGORY_CODEC = enumCodec(MobEffectCategory.class);

    public static final Codec<MobEffectInstance> MOB_EFFECT_INSTANCE_CODEC = CompoundTag.CODEC.xmap(MobEffectInstance::load,
            instance -> instance.save(new CompoundTag()));


    public static <E extends Enum<E>> Codec<E> enumCodec(Class<E> eClass) {
        return Codec.STRING.xmap(s -> Enum.valueOf(eClass,s), Enum::name);
    }

    public static MobEffectInstance from(FriendlyByteBuf pBuffer) {
        MobEffect effect = pBuffer.readById(Registry.MOB_EFFECT);
        int effectAmplifier = pBuffer.readByte();
        int effectDurationTicks = pBuffer.readVarInt();
        byte flags = pBuffer.readByte();

        boolean isEffectVisible =  (flags & 0b10) != 0;

        boolean isEffectAmbient = (flags & 0b1) != 0;

        boolean effectShowsIcon = (flags & 0b100) != 0;

        MobEffectInstance.FactorData factorData = pBuffer.readNullable(buf -> buf.readWithCodec(MobEffectInstance.FactorData.CODEC));
        return new MobEffectInstance(effect, effectDurationTicks, effectAmplifier, isEffectAmbient, isEffectVisible, effectShowsIcon,
                null, Optional.ofNullable(factorData));
    }

    public static void write(MobEffectInstance instance,FriendlyByteBuf pBuffer) {
        pBuffer.writeId(Registry.MOB_EFFECT, instance.getEffect());
        pBuffer.writeByte(instance.getAmplifier());
        pBuffer.writeVarInt(instance.getDuration());

        byte flags = 0;
        if (instance.isAmbient()) {
            flags = (byte)(flags | 0b1);
        }

        if (instance.isVisible()) {
            flags = (byte)(flags | 0b10);
        }

        if (instance.showIcon()) {
            flags = (byte)(flags | 0b100);
        }

        pBuffer.writeByte(flags);
        pBuffer.writeNullable(instance.getFactorData().orElse(null), (buf, data) -> buf.writeWithCodec(MobEffectInstance.FactorData.CODEC, data));
    }

    public static void writeTag(FriendlyByteBuf buf, TagKey<Biome> tagKey) {
        buf.writeResourceKey(ResourceKey.create(Registry.BIOME_REGISTRY,tagKey.location()));
    }

    public static TagKey<Biome> readTag(FriendlyByteBuf buf) {
        ResourceKey<Biome> biomeResourceKey = buf.readResourceKey(Registry.BIOME_REGISTRY);
        return TagKey.create(Registry.BIOME_REGISTRY,biomeResourceKey.location());
    }
}
