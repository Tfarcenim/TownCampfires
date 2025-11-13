package tfar.towncampfires.data.quest;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nullable;
import java.util.Map;

public class QuestCriteria<T extends CriterionTriggerInstance> {
    @Nullable
    private final CriterionTrigger<T> trigger;
    private final T triggerInstance;
    private final Component desc;
    private final int count;

    public QuestCriteria(CriterionTrigger<T> trigger, T triggerInstance, Component desc,int count) {
        this.trigger = trigger;
        this.triggerInstance = triggerInstance;
        this.desc = desc;
        this.count = count;
    }

    public QuestCriteria(Component desc,int count) {
        this(null,null, desc,count);
    }

    public T triggerInstance() {
        return triggerInstance;
    }

    public CriterionTrigger<T> trigger() {
        return trigger;
    }

    public Component desc() {
        return desc;
    }

    public int count() {
        return count;
    }

    public void serializeToNetwork(FriendlyByteBuf pBuffer) {
        pBuffer.writeComponent(desc);
        pBuffer.writeInt(count);
    }

    public static QuestCriteria<?> criterionFromJson(JsonObject pJson, DeserializationContext pContext) {
        ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(pJson, "trigger"));
        CriterionTrigger<?> criteriontrigger = CriteriaTriggers.getCriterion(resourcelocation);
        Component component = Component.Serializer.fromJson(pJson.get("desc").getAsString());
        int count = GsonHelper.getAsInt(pJson,"count",1);
        if (criteriontrigger == null) {
            throw new JsonSyntaxException("Invalid criterion trigger: " + resourcelocation);
        } else {
            CriterionTriggerInstance criteriontriggerinstance = criteriontrigger.createInstance(GsonHelper.getAsJsonObject(pJson, "conditions", new JsonObject()), pContext);
            return new QuestCriteria(criteriontrigger,criteriontriggerinstance,component,count);
        }
    }

    public static QuestCriteria<?> criterionFromNetwork(FriendlyByteBuf buf) {
        Component desc = buf.readComponent();
        int count = buf.readInt();
        return new QuestCriteria<>(desc,count);
    }

    public static Map<String, QuestCriteria<?>> criteriaFromJson(JsonObject pJson, DeserializationContext pContext) {
        Map<String, QuestCriteria<?>> map = Maps.newHashMap();

        for(Map.Entry<String, JsonElement> entry : pJson.entrySet()) {
            map.put(entry.getKey(), criterionFromJson(GsonHelper.convertToJsonObject(entry.getValue(), "criterion"), pContext));
        }

        return map;
    }

    public static Map<String, QuestCriteria<?>> criteriaFromNetwork(FriendlyByteBuf pBuffer) {
        return pBuffer.readMap(FriendlyByteBuf::readUtf, QuestCriteria::criterionFromNetwork);
    }

    public static void serializeToNetwork(Map<String, QuestCriteria<?>> pCriteria, FriendlyByteBuf pBuffer) {
        pBuffer.writeMap(pCriteria, FriendlyByteBuf::writeUtf, (p_145258_, p_145259_) -> {
            p_145259_.serializeToNetwork(p_145258_);
        });
    }

    public JsonElement serializeToJson() {
        if (this.trigger == null) {
            throw new JsonSyntaxException("Missing trigger");
        } else {
            JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("trigger", this.triggerInstance.getCriterion().toString());
            JsonObject jsonobject1 = this.triggerInstance.serializeToJson(SerializationContext.INSTANCE);
            if (jsonobject1.size() != 0) {
                jsonobject.add("conditions", jsonobject1);
            }

            jsonobject.addProperty("desc",Component.Serializer.toJson(desc));
            jsonobject.addProperty("count",count);

            return jsonobject;
        }
    }


}
