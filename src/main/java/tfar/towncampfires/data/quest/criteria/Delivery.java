package tfar.towncampfires.data.quest.criteria;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;

public record Delivery(Ingredient ingredient, int required) {

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("ingredient",ingredient.toJson());
        jsonObject.addProperty("required",required);
        return jsonObject;
    }

    public static Delivery fromJson(JsonObject jsonObject) {
        Ingredient ingredient1 = Ingredient.fromJson(jsonObject.get("ingredient"));
        int count = GsonHelper.getAsInt(jsonObject,"required",1);
        return new Delivery(ingredient1,count);
    }

    public void toPacket(FriendlyByteBuf buf) {
        ingredient.toNetwork(buf);
        buf.writeInt(required);
    }

    public static Delivery fromPacket(FriendlyByteBuf buf) {
        Ingredient ingredient1 = Ingredient.fromNetwork(buf);
        int required = buf.readInt();
        return new Delivery(ingredient1,required);
    }

}
