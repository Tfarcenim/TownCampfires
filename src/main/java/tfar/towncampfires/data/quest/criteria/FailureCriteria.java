package tfar.towncampfires.data.quest.criteria;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import tfar.towncampfires.data.quest.QuestCriteria;

import java.util.ArrayList;
import java.util.List;

// Death: upon death of is the quest marked as a failure (bool)
// Respawn: upon respawning this quest is considered a failure (bool)
// Time: Time accounted in seconds or whatever metric works better (ticks). if this goes to 0 the quest is considered failed
// Distance: Distance passed from quest accepted, mostly meant to keep players inside the distance.
// Recieving Buff/Debuff: if this buff/debuff is acquired the quest is considered failed.
// Game Stage: Recieving this gamestage results in a failed quest.
// Village Return: Returning to any campfire results in this quest being failed.
public record FailureCriteria(List<Pair<QuestCriteria<?>, Integer>> custom, boolean death, long timer, List<MobEffect> forbiddenBuffs,List<String> forbiddenStages) {


    public static final FailureCriteria EMPTY = new FailureCriteria(new ArrayList<>(),false,-1,List.of(),List.of());

    public JsonObject serializeToJson() {
        JsonObject jsonObject = new JsonObject();
        if (!custom.isEmpty()) {
            JsonArray jsonArray = new JsonArray(custom.size());
            for (Pair<QuestCriteria<?>, Integer> criteria : custom) {
                JsonObject o = new JsonObject();
                o.add("trigger", criteria.getFirst().serializeToJson());
                o.addProperty("count", criteria.getSecond());
                jsonArray.add(o);
            }
            jsonObject.add("custom", jsonArray);
        }
        jsonObject.addProperty("death", death);
        jsonObject.addProperty("timer",timer);

        if (!forbiddenBuffs.isEmpty()) {
            JsonArray jsonArray = new JsonArray(forbiddenBuffs.size());
            forbiddenBuffs.forEach(effect -> jsonArray.add(Registry.MOB_EFFECT.getKey(effect).toString()));
            jsonObject.add("forbidden_buffs",jsonArray);
        }

        if (!forbiddenStages.isEmpty()) {
            JsonArray jsonArray = new JsonArray(forbiddenStages.size());
            forbiddenStages.forEach(jsonArray::add);
            jsonObject.add("forbidden_stages",jsonArray);
        }

        return jsonObject;
    }

    public static FailureCriteria deserialize(JsonObject jsonObject, DeserializationContext context) {
        JsonArray customCriteria = GsonHelper.getAsJsonArray(jsonObject, "custom", new JsonArray());

        List<Pair<QuestCriteria<?>, Integer>> criterias = new ArrayList<>(customCriteria.size());

        for (JsonElement element : customCriteria) {
            JsonObject o = element.getAsJsonObject();
            QuestCriteria<?> questCriteria = QuestCriteria.criterionFromJson(o.get("trigger").getAsJsonObject(), context);
            int count = GsonHelper.getAsInt(o, "count", 1);
            criterias.add(Pair.of(questCriteria, count));
        }

        boolean death = GsonHelper.getAsBoolean(jsonObject,"death",false);
        long timer = GsonHelper.getAsLong(jsonObject,"timer",-1);

        JsonArray jsonArrayEffects = GsonHelper.getAsJsonArray(jsonObject,"forbidden_effects",new JsonArray());
        List<MobEffect> effects = new ArrayList<>();
        for (JsonElement element : jsonArrayEffects) {
            effects.add(Registry.MOB_EFFECT.get(new ResourceLocation(element.getAsString())));
        }

        List<String> stages = new ArrayList<>();
        for (JsonElement element : jsonArrayEffects) {
            stages.add(element.getAsString());
        }


        return new FailureCriteria(criterias,death,timer,effects,stages);
    }

    public void serializeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeCollection(custom, (buf1, questCriteriaIntegerPair) -> {
            questCriteriaIntegerPair.getFirst().serializeToNetwork(buf1);
            buf1.writeInt(questCriteriaIntegerPair.getSecond());
        });
        buffer.writeBoolean(death);
        buffer.writeLong(timer);
    }

    public static FailureCriteria fromNetwork(FriendlyByteBuf pBuffer) {
        List<Pair<QuestCriteria<?>, Integer>> pairs = pBuffer.readList(buf1 -> Pair.of(QuestCriteria.criterionFromNetwork(buf1), buf1.readInt()));
        boolean death = pBuffer.readBoolean();
        long timer = pBuffer.readLong();
        return new FailureCriteria(pairs, death,timer,List.of(),List.of());
    }


}
