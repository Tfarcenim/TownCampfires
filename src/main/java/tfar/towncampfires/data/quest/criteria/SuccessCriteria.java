package tfar.towncampfires.data.quest.criteria;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import tfar.towncampfires.data.quest.Quest;
import tfar.towncampfires.data.quest.QuestCriteria;

import java.util.ArrayList;
import java.util.List;

public record SuccessCriteria(List<QuestCriteria<?>> custom,List<Delivery> deliveries) {

    public static SuccessCriteria deserialize(JsonObject jsonObject, DeserializationContext context) {
        JsonArray customCriteria = GsonHelper.getAsJsonArray(jsonObject, "custom", new JsonArray());

        List<QuestCriteria<?>> criterias = new ArrayList<>(customCriteria.size());

        for (JsonElement element : customCriteria) {
            JsonObject o = element.getAsJsonObject();
            QuestCriteria<?> questCriteria = QuestCriteria.criterionFromJson(o, context);
            criterias.add(questCriteria);
        }

        List<Delivery> deliveries1 = new ArrayList<>();
        JsonArray deliveryCriteria = GsonHelper.getAsJsonArray(jsonObject,"delivery",new JsonArray());
        for (JsonElement element : deliveryCriteria) {
            JsonObject o = element.getAsJsonObject();
            Delivery delivery = Delivery.fromJson(o);
            deliveries1.add(delivery);
        }

        return new SuccessCriteria(criterias,deliveries1);
    }

    public void serializeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeCollection(custom, (buf1, questCriteria) -> questCriteria.serializeToNetwork(buf1));
        buffer.writeCollection(deliveries,(buf, delivery) -> delivery.toPacket(buf));
    }

    public static SuccessCriteria fromNetwork(FriendlyByteBuf pBuffer) {
        List<QuestCriteria<?>> pairs = pBuffer.readList(QuestCriteria::criterionFromNetwork);
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