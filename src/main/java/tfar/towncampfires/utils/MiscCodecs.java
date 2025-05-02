package tfar.towncampfires.utils;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectCategory;

public class MiscCodecs {
    public static final Codec<Component> COMPONENT_CODEC = Codec.STRING.xmap(Component.Serializer::fromJson, Component.Serializer::toJson);
    public static final Codec<MobEffectCategory> MOB_EFFECT_CATEGORY_CODEC = codec(MobEffectCategory.class);

    public static <E extends Enum<E>> Codec<E> codec(Class<E> eClass) {
        return Codec.STRING.xmap(s -> Enum.valueOf(eClass,s), Enum::name);
    }

}
