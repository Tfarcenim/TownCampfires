package tfar.towncampfires.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record QuestLogEntry(Component log, ResourceLocation questID) {

    public QuestLogEntry(Component log) {
        this(log,new ResourceLocation("null","null"));
    }

    public static QuestLogEntry fromPacket(FriendlyByteBuf buf) {
        Component component = buf.readComponent();
        ResourceLocation id = buf.readResourceLocation();
        return new QuestLogEntry(component,id);
    }

    public void toPacket(FriendlyByteBuf buf) {
        buf.writeComponent(log);
        buf.writeResourceLocation(questID);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("log",Component.Serializer.toJson(log));
        tag.putString("quest_id",questID.toString());
        return tag;
    }

    public static QuestLogEntry fromNBT(CompoundTag tag) {
        return new QuestLogEntry(Component.Serializer.fromJson(tag.getString("log")),new ResourceLocation(tag.getString("quest_id")));
    }
}
