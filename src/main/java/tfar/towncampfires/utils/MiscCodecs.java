package tfar.towncampfires.utils;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

public class MiscCodecs {
    public static final Codec<Component> COMPONENT_CODEC = Codec.STRING.xmap(Component.Serializer::fromJson, Component.Serializer::toJson);
}
