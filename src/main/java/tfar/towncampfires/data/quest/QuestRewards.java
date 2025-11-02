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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.compat.GameStagesCompat;
import tfar.towncampfires.compat.LoadedMods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuestRewards {


    public static final QuestRewards EMPTY = new QuestRewards(0, 0, new ResourceLocation[0], new ResourceLocation[0],
            CommandFunction.CacheableFunction.NONE, new String[0], new String[0], List.of(), List.of());
    private final int playerExperience;
    private final int campfireExperience;
    private final ResourceLocation[] loot;
    private final ResourceLocation[] recipes;
    private final CommandFunction.CacheableFunction function;
    private final String[] addGameStages;
    private final String[] removeGameStages;
    private final List<MobEffectInstance> applyBuffs;
    private final List<MobEffect> removeBuffs;

    public QuestRewards(int playerExperience, int campfireExperience, ResourceLocation[] pLoot, ResourceLocation[] pRecipes,
                        CommandFunction.CacheableFunction pFunction, String[] addGameStages, String[] removeGameStages, List<MobEffectInstance> applyBuffs,
                        List<MobEffect> removeBuffs) {
        this.playerExperience = playerExperience;
        this.campfireExperience = campfireExperience;
        this.loot = pLoot;
        this.recipes = pRecipes;
        this.function = pFunction;
        this.addGameStages = addGameStages;
        this.removeGameStages = removeGameStages;
        this.applyBuffs = applyBuffs;
        this.removeBuffs = removeBuffs;
    }


    //Completed Added Items:items added upon completing the quest.
    // Failed Added Items: items added upon failing the quest.
    // Completed Removed Items: items removed upon completing the quest.
    // Failed Removed Items: items removed upon failing the quest.
    // Completed Applied Buffs: applied buffs/debuffs upon completing the quest.
    // Failed Applied Buffs: applies buffs/debuffs upon failing the quest.
    // Completed Remove Buffs: buffs/debuffs removed upon completing the quest.
    // Failed Remove Buffs: buffs/debuffs removed upon failing the quest.

    public int playerExperience() {
        return playerExperience;
    }

    public int campfireExperience() {
        return campfireExperience;
    }

    public ResourceLocation[] getRecipes() {
        return this.recipes;
    }

    public void grant(ServerPlayer pPlayer, TownCampfire campfire, boolean isLevelup) {
        campfire.giveExperiencePoints(campfireExperience, isLevelup);
        handleCommon(pPlayer);
    }

    void handleCommon(ServerPlayer player) {
        player.giveExperiencePoints(this.playerExperience);
        //campfire.giveExperiencePoints(campfireExperience,false);
        LootContext lootcontext = new LootContext.Builder(player.getLevel()).withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.ORIGIN, player.position()).withRandom(player.getRandom())
                .withLuck(player.getLuck()).create(LootContextParamSets.ADVANCEMENT_REWARD); // FORGE: luck to LootContext
        boolean addedItems = false;

        for (ResourceLocation resourcelocation : this.loot) {
            for (ItemStack itemstack : player.server.getLootTables().get(resourcelocation).getRandomItems(lootcontext)) {
                if (player.addItem(itemstack)) {
                    player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                    addedItems = true;
                } else {
                    ItemEntity itementity = player.drop(itemstack, false);
                    if (itementity != null) {
                        itementity.setNoPickUpDelay();
                        itementity.setOwner(player.getUUID());
                    }
                }
            }
        }

        if (addedItems) {
            player.containerMenu.broadcastChanges();
        }

        if (this.recipes.length > 0) {
            player.awardRecipesByKey(this.recipes);
        }

        MinecraftServer minecraftserver = player.server;
        this.function.get(minecraftserver.getFunctions()).ifPresent((p_9996_) -> {
            minecraftserver.getFunctions().execute(p_9996_, player.createCommandSourceStack().withSuppressedOutput().withPermission(2));
        });

        if (LoadedMods.gamestages.loaded) {
            GameStagesCompat.onPlayersCompleteQuest(player, this);
        }
        for (MobEffectInstance mobEffectInstance : applyBuffs) {
            player.addEffect(mobEffectInstance);
        }
        for (MobEffect effect : removeBuffs) {
            player.removeEffect(effect);
        }
    }

    public void punish(ServerPlayer pPlayer, TownCampfire campfire) {
        handleCommon(pPlayer);
        //campfire.giveExperiencePoints(campfireExperience,false);
    }


    public String[] getAddGameStages() {
        return addGameStages;
    }

    public String[] getRemoveGameStages() {
        return removeGameStages;
    }

    @Override
    public String toString() {
        return "QuestRewards{" +
                "playerExperience=" + playerExperience +
                ", campfireExperience=" + campfireExperience +
                ", loot=" + Arrays.toString(loot) +
                ", recipes=" + Arrays.toString(recipes) +
                ", function=" + function +
                ", addGameStages=" + Arrays.toString(addGameStages) +
                ", removeGameStages=" + Arrays.toString(removeGameStages) +
                '}';
    }

    public void writeToPacket(FriendlyByteBuf buf) {
        buf.writeInt(playerExperience);
        buf.writeInt(campfireExperience);
    }

    public static QuestRewards readFromPacket(FriendlyByteBuf buf) {
        return new QuestRewards(buf.readInt(), buf.readInt(), new ResourceLocation[0], new ResourceLocation[0],
                CommandFunction.CacheableFunction.NONE, new String[0], new String[0], List.of(), List.of());
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

                for (ResourceLocation resourcelocation : this.loot) {
                    jsonarray.add(resourcelocation.toString());
                }

                jsonobject.add("loot", jsonarray);
            }

            if (this.recipes.length > 0) {
                JsonArray jsonarray1 = new JsonArray();

                for (ResourceLocation resourcelocation1 : this.recipes) {
                    jsonarray1.add(resourcelocation1.toString());
                }

                jsonobject.add("recipes", jsonarray1);
            }

            if (this.function.getId() != null) {
                jsonobject.addProperty("function", this.function.getId().toString());
            }

            if (addGameStages.length > 0) {
                JsonArray jsonArray = new JsonArray();
                for (String s : this.addGameStages) {
                    jsonArray.add(s);
                }
                jsonobject.add(ADD_STAGES, jsonArray);
            }

            if (removeGameStages.length > 0) {
                JsonArray jsonArray = new JsonArray();
                for (String s : this.removeGameStages) {
                    jsonArray.add(s);
                }
                jsonobject.add(REMOVE_STAGES, jsonArray);
            }


            if (!applyBuffs.isEmpty()) {
                JsonArray jsonArray = new JsonArray();
                for (MobEffectInstance effectInstance : applyBuffs) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("forge:id",ForgeRegistries.MOB_EFFECTS.getKey(effectInstance.getEffect()).toString());
                    jsonObject.addProperty("Duration",effectInstance.getDuration());
                    jsonObject.addProperty("Amplifier",effectInstance.getAmplifier());

                    jsonObject.addProperty("Ambient",effectInstance.isAmbient());
                    jsonObject.addProperty("ShowParticles",effectInstance.isVisible());
                    jsonObject.addProperty("ShowIcon",effectInstance.showIcon());

                    jsonArray.add(jsonObject);
                }
                jsonobject.add(ADD_EFFECTS,jsonArray);
            }

            if (!removeBuffs.isEmpty()) {
                JsonArray jsonArray = new JsonArray();
                for (MobEffect effectInstance : removeBuffs) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("forge:id",ForgeRegistries.MOB_EFFECTS.getKey(effectInstance).toString());

                    jsonArray.add(jsonObject);
                }
                jsonobject.add(REMOVE_EFFECTS,jsonArray);
            }

            return jsonobject;
        }
    }

    static String ADD_STAGES = "add_gamestages";
    static String REMOVE_STAGES = "remove_gamestages";

    static String ADD_EFFECTS = "add_effects";
    static String REMOVE_EFFECTS = "remove_effects";

    public static QuestRewards deserialize(JsonObject pJson) throws JsonParseException {
        if (pJson == null) return EMPTY;
        int experience = GsonHelper.getAsInt(pJson, "player_experience", 0);
        int campfireExperience = GsonHelper.getAsInt(pJson, "campfire_experience", 0);
        JsonArray jsonarray = GsonHelper.getAsJsonArray(pJson, "loot", new JsonArray());
        ResourceLocation[] aresourcelocation = new ResourceLocation[jsonarray.size()];

        for (int j = 0; j < aresourcelocation.length; ++j) {
            aresourcelocation[j] = new ResourceLocation(GsonHelper.convertToString(jsonarray.get(j), "loot[" + j + "]"));
        }

        JsonArray jsonarray1 = GsonHelper.getAsJsonArray(pJson, "recipes", new JsonArray());
        ResourceLocation[] aresourcelocation1 = new ResourceLocation[jsonarray1.size()];

        for (int k = 0; k < aresourcelocation1.length; ++k) {
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

        JsonArray jsonArrayAddEffects = GsonHelper.getAsJsonArray(pJson, ADD_EFFECTS, new JsonArray());
        List<MobEffectInstance> addEffects = getAddEffects(jsonArrayAddEffects);

        JsonArray jsonArrayRemoveEffects = GsonHelper.getAsJsonArray(pJson, REMOVE_EFFECTS, new JsonArray());
        List<MobEffect> removeEffects = getRemoveEffects(jsonArrayRemoveEffects);

        return new QuestRewards(experience, campfireExperience, aresourcelocation, aresourcelocation1, commandfunction$cacheablefunction, addStages, removeStages, addEffects, removeEffects);
    }

    //   public static MobEffectInstance load(CompoundTag pNbt) {
    //      int i = pNbt.getByte("Id") & 0xFF;
    //      MobEffect mobeffect = MobEffect.byId(i);
    //      mobeffect = net.minecraftforge.common.ForgeHooks.loadMobEffect(pNbt, "forge:id", mobeffect);
    //      return mobeffect == null ? null : loadSpecifiedEffect(mobeffect, pNbt);
    //   }

    //      int i = pNbt.getByte("Amplifier");
    //      int j = pNbt.getInt("Duration");
    //      boolean flag = pNbt.getBoolean("Ambient");
    //      boolean flag1 = true;
    //      if (pNbt.contains("ShowParticles", 1)) {
    //         flag1 = pNbt.getBoolean("ShowParticles");
    //      }
    //
    //      boolean flag2 = flag1;
    //      if (pNbt.contains("ShowIcon", 1)) {
    //         flag2 = pNbt.getBoolean("ShowIcon");
    //      }

    public static List<MobEffectInstance> getAddEffects(JsonArray array) {
        List<MobEffectInstance> list = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            MobEffect mobeffect = getMobEffect(object);
            if (mobeffect != null) {
                int amplifier = GsonHelper.getAsInt(object,"Amplifier",0);
                int duration = GsonHelper.getAsInt(object,"Duration",600);
                boolean ambient = GsonHelper.getAsBoolean(object,"Ambient",false);

                boolean showParticles = GsonHelper.getAsBoolean(object,"ShowParticles",true);

                boolean showIcon = GsonHelper.getAsBoolean(object,"ShowIcon",true);


                list.add(new MobEffectInstance(mobeffect,duration,amplifier,ambient,showParticles,showIcon));
                //support hidden effects?
            }
        }
        return list;
    }

    @Nullable
    public static MobEffect getMobEffect(JsonObject object) {
        int id = GsonHelper.getAsInt(object,"Id",0);
        MobEffect mobeffect = MobEffect.byId(id);

        if (object.has("forge:id")) {
            mobeffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(object.get("forge:id").getAsString()));
        }
        return mobeffect;
    }

    public static List<MobEffect> getRemoveEffects(JsonArray array) {
        List<MobEffect> list = new ArrayList<>();
        for (JsonElement element : array) {
            MobEffect mobeffect = getMobEffect(element.getAsJsonObject());
            if (mobeffect != null) {
                list.add(mobeffect);
            }
        }
        return list;
    }

    public static String[] toArray(JsonArray array) {
        String[] strings = new String[array.size()];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = array.get(i).getAsString();
        }
        return strings;
    }
}
