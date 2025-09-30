package tfar.towncampfires.data.quest;

import com.google.gson.*;
import net.minecraft.commands.CommandFunction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.compat.GameStagesCompat;
import tfar.towncampfires.compat.LoadedMods;

import java.util.Arrays;

public class QuestRewards {


    public static final QuestRewards EMPTY = new QuestRewards(0,0, new ResourceLocation[0], new ResourceLocation[0],
            CommandFunction.CacheableFunction.NONE,new String[0],new String[0]);
    private final int playerExperience;
    private final int campfireExperience;
    private final ResourceLocation[] loot;
    private final ResourceLocation[] recipes;
    private final CommandFunction.CacheableFunction function;
    private final String[] addGameStages;
    private final String[] removeGameStages;

    public QuestRewards(int playerExperience,int campfireExperience, ResourceLocation[] pLoot, ResourceLocation[] pRecipes,
                        CommandFunction.CacheableFunction pFunction,String[] addGameStages,String[] removeGameStages) {
        this.playerExperience = playerExperience;
        this.campfireExperience = campfireExperience;
        this.loot = pLoot;
        this.recipes = pRecipes;
        this.function = pFunction;
        this.addGameStages = addGameStages;
        this.removeGameStages = removeGameStages;
    }

    public int playerExperience() {
        return playerExperience;
    }

    public int campfireExperience() {
        return campfireExperience;
    }

    public ResourceLocation[] getRecipes() {
        return this.recipes;
    }

    public void grant(ServerPlayer pPlayer, TownCampfire campfire,boolean isLevelup) {
        pPlayer.giveExperiencePoints(this.playerExperience);
        campfire.giveExperiencePoints(campfireExperience,isLevelup);
        LootContext lootcontext = (new LootContext.Builder(pPlayer.getLevel())).withParameter(LootContextParams.THIS_ENTITY, pPlayer)
                .withParameter(LootContextParams.ORIGIN, pPlayer.position()).withRandom(pPlayer.getRandom()).withLuck(pPlayer.getLuck())
                .create(LootContextParamSets.ADVANCEMENT_REWARD); // FORGE: luck to LootContext
        boolean flag = false;

        for(ResourceLocation resourcelocation : this.loot) {
            for(ItemStack itemstack : pPlayer.server.getLootTables().get(resourcelocation).getRandomItems(lootcontext)) {
                if (pPlayer.addItem(itemstack)) {
                    pPlayer.level.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.ITEM_PICKUP,
                            SoundSource.PLAYERS, 0.2F, ((pPlayer.getRandom().nextFloat() - pPlayer.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                    flag = true;
                } else {
                    ItemEntity itementity = pPlayer.drop(itemstack, false);
                    if (itementity != null) {
                        itementity.setNoPickUpDelay();
                        itementity.setOwner(pPlayer.getUUID());
                    }
                }
            }
        }

        if (flag) {
            pPlayer.containerMenu.broadcastChanges();
        }

        if (this.recipes.length > 0) {
            pPlayer.awardRecipesByKey(this.recipes);
        }

        MinecraftServer server = pPlayer.server;
        this.function.get(server.getFunctions()).ifPresent(function -> {
            server.getFunctions().execute(function, pPlayer.createCommandSourceStack().withSuppressedOutput().withPermission(2));
        });
    }

    public void punish(ServerPlayer pPlayer, TownCampfire campfire) {
        pPlayer.giveExperiencePoints(this.playerExperience);
        //campfire.giveExperiencePoints(campfireExperience,false);
        LootContext lootcontext = new LootContext.Builder(pPlayer.getLevel()).withParameter(LootContextParams.THIS_ENTITY, pPlayer)
                .withParameter(LootContextParams.ORIGIN, pPlayer.position()).withRandom(pPlayer.getRandom())
                .withLuck(pPlayer.getLuck()).create(LootContextParamSets.ADVANCEMENT_REWARD); // FORGE: luck to LootContext
        boolean addedItems = false;

        for(ResourceLocation resourcelocation : this.loot) {
            for(ItemStack itemstack : pPlayer.server.getLootTables().get(resourcelocation).getRandomItems(lootcontext)) {
                if (pPlayer.addItem(itemstack)) {
                    pPlayer.level.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((pPlayer.getRandom().nextFloat() - pPlayer.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                    addedItems = true;
                } else {
                    ItemEntity itementity = pPlayer.drop(itemstack, false);
                    if (itementity != null) {
                        itementity.setNoPickUpDelay();
                        itementity.setOwner(pPlayer.getUUID());
                    }
                }
            }
        }

        if (addedItems) {
            pPlayer.containerMenu.broadcastChanges();
        }

        if (this.recipes.length > 0) {
            pPlayer.awardRecipesByKey(this.recipes);
        }

        MinecraftServer minecraftserver = pPlayer.server;
        this.function.get(minecraftserver.getFunctions()).ifPresent((p_9996_) -> {
            minecraftserver.getFunctions().execute(p_9996_, pPlayer.createCommandSourceStack().withSuppressedOutput().withPermission(2));
        });

        if (LoadedMods.gamestages.loaded) {
            GameStagesCompat.onPlayersCompleteQuest(pPlayer,this);
        }
    }


    public String[] getAddGameStages() {
        return addGameStages;
    }

    public String[] getRemoveGameStages() {
        return removeGameStages;
    }

    public String toString() {
        return "QuestRewards{experience=" + this.playerExperience + ", loot=" + Arrays.toString(this.loot) +
                ", recipes=" + Arrays.toString(this.recipes) + ", function=" + this.function + "}";
    }

    public void writeToPacket(FriendlyByteBuf buf) {
        buf.writeInt(playerExperience);
        buf.writeInt(campfireExperience);
    }

    public static QuestRewards readFromPacket(FriendlyByteBuf buf) {
        return new QuestRewards(buf.readInt(),buf.readInt(),new ResourceLocation[0],new ResourceLocation[0], CommandFunction.CacheableFunction.NONE,new String[0],new String[0]);
    }

    public JsonElement serializeToJson() {
        if (this == EMPTY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonobject = new JsonObject();
            if (this.playerExperience != 0) {
                jsonobject.addProperty("player_experience", this.playerExperience);
            }

            if (this.campfireExperience != 0) {
                jsonobject.addProperty("campfire_experience", this.campfireExperience);
            }

            if (this.loot.length > 0) {
                JsonArray jsonarray = new JsonArray();

                for(ResourceLocation resourcelocation : this.loot) {
                    jsonarray.add(resourcelocation.toString());
                }

                jsonobject.add("loot", jsonarray);
            }

            if (this.recipes.length > 0) {
                JsonArray jsonarray1 = new JsonArray();

                for(ResourceLocation resourcelocation1 : this.recipes) {
                    jsonarray1.add(resourcelocation1.toString());
                }

                jsonobject.add("recipes", jsonarray1);
            }

            if (this.function.getId() != null) {
                jsonobject.addProperty("function", this.function.getId().toString());
            }

            if (addGameStages.length > 0) {
                JsonArray jsonArray = new JsonArray();
                for(String s : this.addGameStages) {
                    jsonArray.add(s);
                }
                jsonobject.add(ADD_STAGES, jsonArray);
            }

            if (removeGameStages.length > 0) {
                JsonArray jsonArray = new JsonArray();
                for(String s : this.removeGameStages) {
                    jsonArray.add(s);
                }
                jsonobject.add(REMOVE_STAGES, jsonArray);
            }

            return jsonobject;
        }
    }

    static String ADD_STAGES = "add_gamestages";
    static String REMOVE_STAGES = "remove_gamestages";

    public static QuestRewards deserialize(JsonObject pJson) throws JsonParseException {
        if(pJson == null) return EMPTY;
        int experience = GsonHelper.getAsInt(pJson, "player_experience", 0);
        int campfireExperience = GsonHelper.getAsInt(pJson, "campfire_experience", 0);
        JsonArray jsonarray = GsonHelper.getAsJsonArray(pJson, "loot", new JsonArray());
        ResourceLocation[] aresourcelocation = new ResourceLocation[jsonarray.size()];

        for(int j = 0; j < aresourcelocation.length; ++j) {
            aresourcelocation[j] = new ResourceLocation(GsonHelper.convertToString(jsonarray.get(j), "loot[" + j + "]"));
        }

        JsonArray jsonarray1 = GsonHelper.getAsJsonArray(pJson, "recipes", new JsonArray());
        ResourceLocation[] aresourcelocation1 = new ResourceLocation[jsonarray1.size()];

        for(int k = 0; k < aresourcelocation1.length; ++k) {
            aresourcelocation1[k] = new ResourceLocation(GsonHelper.convertToString(jsonarray1.get(k), "recipes[" + k + "]"));
        }

        CommandFunction.CacheableFunction commandfunction$cacheablefunction;
        if (pJson.has("function")) {
            commandfunction$cacheablefunction = new CommandFunction.CacheableFunction(new ResourceLocation(GsonHelper.getAsString(pJson, "function")));
        } else {
            commandfunction$cacheablefunction = CommandFunction.CacheableFunction.NONE;
        }

        JsonArray jsonarrayAdd = GsonHelper.getAsJsonArray(pJson, ADD_STAGES, new JsonArray());
        String[] addStages = toArray(jsonarrayAdd);

        JsonArray jsonarrayRemove = GsonHelper.getAsJsonArray(pJson, REMOVE_STAGES, new JsonArray());
        String[] removeStages = toArray(jsonarrayRemove);

        return new QuestRewards(experience,campfireExperience, aresourcelocation, aresourcelocation1, commandfunction$cacheablefunction,addStages,removeStages);
    }

    public static String[] toArray(JsonArray array) {
        String[] strings = new String[array.size()];
        for (int i = 0; i < strings.length;i++)  {
            strings[i] = array.get(i).getAsString();
        }
        return strings;
    }
}
