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
                    QuestAppearanceConditions appearanceConditions,MultiplayerType type,
                    List<Pair<QuestCriteria<?>, Integer>> criterias, QuestRewards rewards,
                    List<Pair<QuestCriteria<?>, Integer>> failureCriterias,QuestPunishments punishments,boolean levelUp) {

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



        JsonElement element4 = MiscCodecs.enumCodec(MultiplayerType.class).encodeStart(JsonOps.INSTANCE, type)
                .resultOrPartial(TownCampfires.LOGGER::error).get();

        jsonObject.add("multiplayer_type", element4);


        jsonObject.add("criteria", writeCriteria(criterias));
        jsonObject.add("rewards", rewards.serializeToJson());

        jsonObject.add("failure_criteria", writeCriteria(failureCriterias));
        jsonObject.add("punishments", punishments.serializeToJson());

        jsonObject.addProperty("level_up",levelUp);

        return jsonObject;
    }

    JsonArray writeCriteria(List<Pair<QuestCriteria<?>,Integer>> criterias) {
        JsonArray jsonArray = new JsonArray(criterias().size());
        for (Pair<QuestCriteria<?>, Integer> criteria : criterias) {
            JsonObject o = new JsonObject();
            o.add("trigger", criteria.getFirst().serializeToJson());
            o.addProperty("count", criteria.getSecond());
            jsonArray.add(o);
        }
        return jsonArray;
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

        MultiplayerType multiplayerType = MiscCodecs.enumCodec(MultiplayerType.class).decode(JsonOps.INSTANCE, object.get("multiplayer_type"))
                .resultOrPartial(TownCampfires.LOGGER::error).orElseThrow().getFirst();

        JsonArray jsonArray = object.getAsJsonArray("criteria");

        if (jsonArray.isEmpty()) throw new JsonParseException("Quest must have criteria!");

        var criterias = readCriteria(jsonArray,context);
        QuestRewards rewards = QuestRewards.deserialize(object.get("rewards").getAsJsonObject());

        var failure_criterias = readCriteria(object.getAsJsonArray("failure_criteria"),context);

        QuestPunishments punishments = QuestPunishments.deserialize((JsonObject) object.get("punishments"));

        boolean levelUp = object.get("level_up").getAsBoolean();

        return new Quest(name, icon, desc, appearance_conditions, multiplayerType, criterias, rewards,failure_criterias,punishments,levelUp);
    }

    static List<Pair<QuestCriteria<?>, Integer>> readCriteria(JsonArray jsonArray,DeserializationContext context) {
        List<Pair<QuestCriteria<?>, Integer>> criterias = new ArrayList<>(jsonArray.size());

        for (JsonElement element : jsonArray) {
            JsonObject o = element.getAsJsonObject();
            QuestCriteria<?> questCriteria = QuestCriteria.criterionFromJson(o.get("trigger").getAsJsonObject(), context);
            int count = GsonHelper.getAsInt(o, "count", 1);
            criterias.add(Pair.of(questCriteria, count));
        }
        return criterias;
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
        buf.writeCollection(failureCriterias, (buf1, questCriteriaIntegerPair) -> {
            questCriteriaIntegerPair.getFirst().serializeToNetwork(buf1);
            buf1.writeInt(questCriteriaIntegerPair.getSecond());
        });
        buf.writeBoolean(levelUp);
    }

    public static Quest fromPacket(FriendlyByteBuf buf) {
        return new Quest(buf.readComponent(), buf.readItem(), buf.readList(FriendlyByteBuf::readComponent),
                QuestAppearanceConditions.fromPacket(buf), buf.readEnum(MultiplayerType.class),
                buf.readList(buf1 -> Pair.of(QuestCriteria.criterionFromNetwork(buf1), buf1.readInt())), QuestRewards.readFromPacket(buf),
                buf.readList(buf1 -> Pair.of(QuestCriteria.criterionFromNetwork(buf1), buf1.readInt())),
                QuestPunishments.readFromPacket(buf),buf.readBoolean());
    }

    // Quest MultiplayerType: normal, preparation solo, preparation all or level.
    public enum MultiplayerType {
        solo,preparation_solo,preparation_multiplayer;
    }

}
