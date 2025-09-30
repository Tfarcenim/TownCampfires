package tfar.towncampfires.data.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.data.quest.criteria.FailureCriteria;
import tfar.towncampfires.utils.MiscCodecs;

import java.util.ArrayList;
import java.util.List;



public record Quest(Component name, ItemStack icon, List<Component> desc,
                    QuestAppearanceConditions appearanceConditions, MultiplayerType type,
                    List<Pair<QuestCriteria<?>, Integer>> criterias, QuestRewards rewards,
                    FailureCriteria failureCriteria, QuestRewards punishments,
                    boolean levelUp, long weight, Component compactName, List<Component> compactDesc, int difficulty, int slots, int attempts,
                    List<String> gameStages) {

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

        jsonObject.add("failure_criteria", failureCriteria.serializeToJson());
        jsonObject.add("punishments", punishments.serializeToJson());

        jsonObject.addProperty("level_up",levelUp);
        jsonObject.addProperty("weight",weight);


        JsonElement element5 = MiscCodecs.COMPONENT_CODEC.encodeStart(JsonOps.INSTANCE, compactName)
                .resultOrPartial(TownCampfires.LOGGER::error).get();
        jsonObject.add("compact_name", element5);

        JsonElement element6 = MiscCodecs.COMPONENT_CODEC.listOf().encodeStart(JsonOps.INSTANCE, compactDesc)
                .resultOrPartial(TownCampfires.LOGGER::error).get();
        jsonObject.add("compact_desc", element6);

        jsonObject.addProperty("difficulty",difficulty);
        jsonObject.addProperty("slots",slots);
        jsonObject.addProperty("attempts",attempts);

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

        var failure_criterias = FailureCriteria.deserialize(object.getAsJsonObject("failure_criteria"),context);

        QuestRewards punishments = QuestRewards.deserialize((JsonObject) object.get("punishments"));

        boolean levelUp = GsonHelper.getAsBoolean(object,"level_up",false);
        int weight = GsonHelper.getAsInt(object,"weight",1);

        Component compactName = getOrFallback(object,"compact_name",MiscCodecs.COMPONENT_CODEC,name);
        List<Component> compactDesc = getOrFallback(object,"compact_desc",MiscCodecs.COMPONENT_CODEC.listOf(),desc);

        int difficulty = GsonHelper.getAsInt(object,"difficulty",1);
        int slots = GsonHelper.getAsInt(object,"slots",1);
        int attempts = GsonHelper.getAsInt(object,"attempts",1);

        List<String> questGameStages = getOrFallback(object,"gamestages",Codec.STRING.listOf(),List.of());

        return new Quest(name, icon, desc, appearance_conditions, multiplayerType, criterias, rewards,failure_criterias,
                punishments,levelUp,weight,compactName,compactDesc,difficulty,slots,attempts,questGameStages);
    }

    public static <C> C getOrFallback(JsonObject json,String element,Codec<C> codec,C fallback) {
        if (!json.has(element)) return fallback;
        return codec.decode(JsonOps.INSTANCE, json.get(element))
                .resultOrPartial(TownCampfires.LOGGER::error).orElseThrow().getFirst();
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
        rewards.writeToPacket(buf);
        failureCriteria.serializeToNetwork(buf);
        punishments.writeToPacket(buf);
        buf.writeBoolean(levelUp);
        buf.writeLong(weight);

        buf.writeComponent(compactName);
        buf.writeCollection(compactDesc, FriendlyByteBuf::writeComponent);

        buf.writeInt(difficulty);
        buf.writeInt(slots);
        buf.writeInt(attempts);
    }

    public static Quest fromPacket(FriendlyByteBuf buf) {
        return new Quest(buf.readComponent(), buf.readItem(), buf.readList(FriendlyByteBuf::readComponent),
                QuestAppearanceConditions.fromPacket(buf), buf.readEnum(MultiplayerType.class),
                buf.readList(buf1 -> Pair.of(QuestCriteria.criterionFromNetwork(buf1), buf1.readInt())), QuestRewards.readFromPacket(buf),
                FailureCriteria.fromNetwork(buf),
                QuestRewards.readFromPacket(buf),buf.readBoolean(),buf.readLong(),buf.readComponent(),
                buf.readList(FriendlyByteBuf::readComponent),buf.readInt(),buf.readInt(),buf.readInt(),List.of());
    }

    // Quest MultiplayerType: normal, preparation solo, preparation all or level.
    public enum MultiplayerType {
        solo, prep_solo, prep_mp;
    }

}
