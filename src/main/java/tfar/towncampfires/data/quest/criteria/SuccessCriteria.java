package tfar.towncampfires.data.quest.criteria;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import tfar.towncampfires.data.quest.Quest;
import tfar.towncampfires.data.quest.QuestCriteria;

import java.util.ArrayList;
import java.util.List;

public record SuccessCriteria(List<Pair<QuestCriteria<?>, Integer>> custom,List<Delivery> deliveries) {

    public static SuccessCriteria deserialize(JsonObject jsonObject, DeserializationContext context) {
        JsonArray customCriteria = GsonHelper.getAsJsonArray(jsonObject, "custom", new JsonArray());

        List<Pair<QuestCriteria<?>, Integer>> criterias = new ArrayList<>(customCriteria.size());

        for (JsonElement element : customCriteria) {
            JsonObject o = element.getAsJsonObject();
            QuestCriteria<?> questCriteria = QuestCriteria.criterionFromJson(o.get("trigger").getAsJsonObject(), context);
            int count = GsonHelper.getAsInt(o, "count", 1);
            criterias.add(Pair.of(questCriteria, count));
        }

        List<Delivery> deliveries1 = new ArrayList<>();
        JsonArray deliveryCriteria = GsonHelper.getAsJsonArray(jsonObject,"delivery");
        for (JsonElement element : deliveryCriteria) {
            JsonObject o = element.getAsJsonObject();
            Delivery delivery = Delivery.fromJson(o);
            deliveries1.add(delivery);
        }

        return new SuccessCriteria(criterias,deliveries1);
    }

    public void serializeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeCollection(custom, (buf1, questCriteriaIntegerPair) -> {
            questCriteriaIntegerPair.getFirst().serializeToNetwork(buf1);
            buf1.writeInt(questCriteriaIntegerPair.getSecond());
        });
        buffer.writeCollection(deliveries,(buf, delivery) -> delivery.toPacket(buf));
    }

    public static SuccessCriteria fromNetwork(FriendlyByteBuf pBuffer) {
        List<Pair<QuestCriteria<?>, Integer>> pairs = pBuffer.readList(buf1 -> Pair.of(QuestCriteria.criterionFromNetwork(buf1), buf1.readInt()));
        List<Delivery> deliveries1 = pBuffer.readList(Delivery::fromPacket);
        return new SuccessCriteria(pairs,deliveries1);
    }


    public JsonObject serializeToJson() {
        JsonObject jsonObject = new JsonObject();
        if (!custom.isEmpty()) {
            jsonObject.add("custom", Quest.serializeCriteria(custom));
        }
        if (!deliveries.isEmpty()) {
            JsonArray array = new JsonArray();
            for (Delivery delivery : deliveries) {
                JsonObject o = delivery.toJson();
                array.add(o);
            }
            jsonObject.add("deliveries",array);
        }
        return jsonObject;
    }
}