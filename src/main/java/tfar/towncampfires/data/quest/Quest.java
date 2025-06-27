package tfar.towncampfires.data.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.utils.MiscCodecs;

import java.util.ArrayList;
import java.util.List;

public record Quest(Component name, ItemStack icon, List<Component> desc,
                    QuestAppearanceConditions appearanceConditions, tfar.towncampfires.data.quest.Quest.Type type,
                    List<Pair<QuestCriteria<?>, Integer>> criterias, QuestRewards rewards) {

    //.encodeStart(JsonOps.INSTANCE, quest).resultOrPartial(TownCampfires.LOGGER::error).get().getAsJsonObject();

    public JsonObject write() {
        JsonObject jsonObject = new JsonObject();
        JsonElement element0 = MiscCodecs.COMPONENT_CODEC.encodeStart(JsonOps.INSTANCE, name)
                .resultOrPartial(TownCampfires.LOGGER::error).get();

        jsonObject.add("name", element0);

        JsonElement element1 = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, icon)
                .resultOrPartial(TownCampfires.LOGGER::error).get().getAsJsonObject();

        jsonObject.add("icon", element1);

        JsonElement element2 = MiscCodecs.COMPONENT_CODEC.listOf().encodeStart(JsonOps.INSTANCE, desc)
                .resultOrPartial(TownCampfires.LOGGER::error).get();
        jsonObject.add("desc", element2);

        JsonElement element3 = QuestAppearanceConditions.CODEC.encodeStart(JsonOps.INSTANCE, appearanceConditions)
                .resultOrPartial(TownCampfires.LOGGER::error).get().getAsJsonObject();

        jsonObject.add("appearance_conditions", element3);

        JsonElement element4 = MiscCodecs.enumCodec(Type.class).encodeStart(JsonOps.INSTANCE, type)
                .resultOrPartial(TownCampfires.LOGGER::error).get();

        jsonObject.add("type", element4);

        JsonArray jsonArray = new JsonArray(criterias().size());

        for (Pair<QuestCriteria<?>, Integer> criteria : criterias) {
            JsonObject o = new JsonObject();
            o.add("trigger", criteria.getFirst().serializeToJson());
            o.addProperty("count", criteria.getSecond());
            jsonArray.add(o);
        }

        jsonObject.add("criteria", jsonArray);

        jsonObject.add("rewards", rewards.serializeToJson());

        return jsonObject;
    }

    //Quest.CODEC.decode(JsonOps.INSTANCE, pJson).resultOrPartial(LOGGER::error).orElseThrow().getFirst()

    public static Quest read(JsonObject object, DeserializationContext context) {
        Component name = MiscCodecs.COMPONENT_CODEC.decode(JsonOps.INSTANCE, object.get("name"))
                .resultOrPartial(TownCampfires.LOGGER::error).orElseThrow().getFirst();

        ItemStack icon = ItemStack.CODEC.decode(JsonOps.INSTANCE, object.get("icon"))
                .resultOrPartial(TownCampfires.LOGGER::error).orElseThrow().getFirst();

        List<Component> desc = MiscCodecs.COMPONENT_CODEC.listOf().decode(JsonOps.INSTANCE, object.get("desc"))
                .resultOrPartial(TownCampfires.LOGGER::error).orElseThrow().getFirst();

        QuestAppearanceConditions appearance_conditions = QuestAppearanceConditions.CODEC.decode(JsonOps.INSTANCE, object.get("appearance_conditions"))
                .resultOrPartial(TownCampfires.LOGGER::error).orElseThrow().getFirst();

        Type type = MiscCodecs.enumCodec(Type.class).decode(JsonOps.INSTANCE, object.get("type"))
                .resultOrPartial(TownCampfires.LOGGER::error).orElseThrow().getFirst();

        JsonArray jsonArray = object.getAsJsonArray("criteria");

        if (jsonArray.isEmpty()) throw new JsonParseException("Quest must have criteria!");

        List<Pair<QuestCriteria<?>, Integer>> criterias = new ArrayList<>(jsonArray.size());

        for (JsonElement element : jsonArray) {
            JsonObject o = element.getAsJsonObject();
            QuestCriteria<?> questCriteria = QuestCriteria.criterionFromJson(o.get("trigger").getAsJsonObject(), context);
            int count = GsonHelper.getAsInt(o, "count", 1);
            criterias.add(Pair.of(questCriteria, count));
        }

        QuestRewards rewards = QuestRewards.deserialize(object.get("rewards").getAsJsonObject());

        return new Quest(name, icon, desc, appearance_conditions, type, criterias, rewards);
    }

    public void toPacket(FriendlyByteBuf buf) {
        buf.writeComponent(name);
        buf.writeItem(icon);
        buf.writeCollection(desc, FriendlyByteBuf::writeComponent);
        appearanceConditions.toPacket(buf);
        buf.writeEnum(type);
        buf.writeCollection(criterias, (buf1, questCriteriaIntegerPair) -> {
            questCriteriaIntegerPair.getFirst().serializeToNetwork(buf1);
            buf1.writeInt(questCriteriaIntegerPair.getSecond());
        });
    }

    public static Quest fromPacket(FriendlyByteBuf buf) {
        return new Quest(buf.readComponent(), buf.readItem(), buf.readList(FriendlyByteBuf::readComponent),
                QuestAppearanceConditions.fromPacket(buf), buf.readEnum(Type.class),
                buf.readList(buf1 -> Pair.of(QuestCriteria.criterionFromNetwork(buf1), buf1.readInt())), QuestRewards.readFromPacket(buf));
    }

    // Quest Type: normal, preparation solo, preparation all or level.
    public enum Type {
        normal, preparation_solo, preparation_all, level;
    }

}
