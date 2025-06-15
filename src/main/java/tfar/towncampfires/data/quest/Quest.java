package tfar.towncampfires.data.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import tfar.towncampfires.utils.MiscCodecs;

import java.util.List;

public record Quest(Component name, ItemStack icon, List<Component> desc,
                    QuestAppearanceConditions appearanceConditions, Type type)  {

    public static final Codec<Quest> CODEC = RecordCodecBuilder.create(questInstance ->
        questInstance.group(
                MiscCodecs.COMPONENT_CODEC.fieldOf("name").forGetter(Quest::name),
                ItemStack.CODEC.fieldOf("icon").forGetter(Quest::icon),
                MiscCodecs.COMPONENT_CODEC.listOf().fieldOf("desc").forGetter(Quest::desc),
                QuestAppearanceConditions.CODEC.fieldOf("appearance_conditions").forGetter(Quest::appearanceConditions),
                MiscCodecs.enumCodec(Type.class).fieldOf("type").forGetter(Quest::type)
    ).apply(questInstance,Quest::new));

    public void toPacket(FriendlyByteBuf buf) {
        buf.writeComponent(name);
        buf.writeItem(icon);
        buf.writeCollection(desc, FriendlyByteBuf::writeComponent);
        appearanceConditions.toPacket(buf);
        buf.writeEnum(type);
    }

    public static Quest fromPacket(FriendlyByteBuf buf) {
        return new Quest(buf.readComponent(),buf.readItem(),buf.readList(FriendlyByteBuf::readComponent),
                QuestAppearanceConditions.fromPacket(buf),buf.readEnum(Type.class));
    }

    // Quest Type: normal, preparation solo, preparation all or level.
    public enum Type {
        normal,preparation_solo,preparation_all,level;
    }

}
