package tfar.towncampfires;

import com.mojang.logging.LogUtils;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.PredicateManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import tfar.towncampfires.client.TownCampfiresClient;
import tfar.towncampfires.config.TownCampfireConfig;
import tfar.towncampfires.data.CampfireEffectLoader;
import tfar.towncampfires.data.quest.QuestLoader;
import tfar.towncampfires.datagen.ModDatagen;
import tfar.towncampfires.init.*;
import tfar.towncampfires.mixin.BlockEntityTypeAccessor;
import tfar.towncampfires.network.ForgePacketHandler;
import tfar.towncampfires.network.PacketHandler;
import tfar.towncampfires.network.client.S2CCampfireEffectPacket;
import tfar.towncampfires.network.client.S2CModPacket;
import tfar.towncampfires.network.client.S2CQuestPacket;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TownCampfires.MODID)
public class TownCampfires
{//nothing too specific but
    // orange,
    // red -30 hue,
    // light blue 180 hue +30 lightness,
    // green 80 hue -25 lightness,
    // and gray

    public static Map<Registry<?>, List<Pair<ResourceLocation, Supplier<?>>>> registerLater = new HashMap<>();

    public static final String MODID = "towncampfires";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public static CampfireEffectLoader campfireEffectLoader;
    public static QuestLoader questLoader;


    public TownCampfires() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, TownCampfireConfig.SERVER_SPEC);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        // Register the setup method for modloading
        bus.addListener(this::setup);
        bus.addListener(this::register);
        bus.addListener(ModDatagen::gather);
        // Register ourselves for server and other game events we are interested in
        if (FMLEnvironment.dist.isClient()) {
            TownCampfiresClient.init(bus);
        }
        registerAll(ModBlocks.class,Registry.BLOCK, Block.class);
        registerAll(ModItems.class,Registry.ITEM, Item.class);
        registerAll(ModBlockEntities.class,Registry.BLOCK_ENTITY_TYPE,(Class<BlockEntityType<?>>)(Object)BlockEntityType.class);
       // MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::useItem);
        MinecraftForge.EVENT_BUS.addListener(this::levelTick);
        MinecraftForge.EVENT_BUS.addListener(this::reloadListeners);
        MinecraftForge.EVENT_BUS.addListener(this::serverStop);
        MinecraftForge.EVENT_BUS.addListener(this::sync);
        MinecraftForge.EVENT_BUS.addListener(this::playerLogin);
    }

    void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        CampfireLevelData campfireLevelData = CampfireLevelData.getOrCreate(player.server.overworld());
        if (campfireLevelData != null) {
                campfireLevelData.sendDataTo(player);
        }
    }

    void sync(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        S2CModPacket effectPacket = new S2CCampfireEffectPacket(campfireEffectLoader.getNonHiddenEffects());
        S2CModPacket questPacket = new S2CQuestPacket(questLoader.getQuestMap());
        if (player != null) {
            ForgePacketHandler.sendToClient(effectPacket,player);
            ForgePacketHandler.sendToClient(questPacket,player);
        }else {
            event.getPlayerList().getPlayers().forEach(player1 -> ForgePacketHandler.sendToClient(effectPacket,player1));
            event.getPlayerList().getPlayers().forEach(player1 -> ForgePacketHandler.sendToClient(questPacket,player1));
        }
    }

    void reloadListeners(AddReloadListenerEvent event) {
        event.addListener(campfireEffectLoader = new CampfireEffectLoader());
        PredicateManager predicateManager = event.getServerResources().getPredicateManager();
        event.addListener(questLoader = new QuestLoader(predicateManager));
    }

    void serverStop(ServerStoppedEvent event) {
        campfireEffectLoader = null;
        questLoader = null;
    }

    void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ServerLevel level = (ServerLevel) event.level;
            CampfireLevelData campfireLevelData = CampfireLevelData.getOrCreate(level);
            campfireLevelData.tick();
        }
    }

    void useItem(PlayerInteractEvent.RightClickBlock event){
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;//allow empty hand interaction
        Player player = event.getEntity();
        if (player.getAbilities().instabuild) return;
        BlockPos pos = event.getPos();
        Level level = event.getLevel();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof TownCampfireBlock) {
            return;
        }
        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) event.getLevel();
            Optional<BlockPos> optional = findTownCampfire(serverLevel, pos, TownCampfireConfig.CONFIG.radius.get());
            if (optional.isPresent()) {
                if (!stack.is(ModTags.USABLE_WITHIN_CAMPFIRE_RANGE)) {
                    event.setCanceled(true);
                    return;
                }

                boolean isWorkbench = stack.is(ModTags.WORKBENCHES);

                BlockPos campPos = optional.get();
                BlockEntity be = serverLevel.getBlockEntity(campPos);
                if (be instanceof TownCampfireBlockEntity townBE) {
                    TownCampfire townCampfire = townBE.townCampfire;

                    if (isWorkbench) {
                        if (townCampfire.hasFreeWorkbenches()) {
                            townCampfire.incrementUsedWorkbenches();
                            CampfireLevelData.getOrCreate(serverLevel).setDirty();
                        } else {
                            event.setCanceled(true);
                        }
                    } else {
                        if (townCampfire.hasFreeBlocks()) {
                            townCampfire.incrementUsedBlocks();
                            CampfireLevelData.getOrCreate(serverLevel).setDirty();
                        } else {
                            event.setCanceled(true);
                        }
                    }
                }
            } else {
                if (!stack.is(ModTags.USABLE_OUTSIDE_OF_CAMPFIRE_RANGE)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    public static Optional<BlockPos> findTownCampfire(ServerLevel serverLevel,BlockPos pPos,int distance) {
        return serverLevel.getPoiManager().findClosest(holder -> holder.value() == ModPOIs.TOWN_CAMPFIRE, pos -> true,
                pPos, distance, PoiManager.Occupancy.ANY);
    }

    static Optional<BlockPos> findBed(ServerLevel serverLevel,BlockPos pPos,int distance) {
        return serverLevel.getPoiManager().findClosest(holder -> holder.value() == ModPOIs.TOWN_CAMPFIRE, pos -> true,
                pPos, distance, PoiManager.Occupancy.ANY);
    }

    public static boolean checkBed(ServerPlayer player,BlockPos bedPos) {
        Optional<BlockPos> campfire = TownCampfires.findTownCampfire(player.getLevel(),bedPos, TownCampfireConfig.CONFIG.radius.get());
        if (campfire.isPresent()) {
            BlockPos playerPos = player.blockPosition();
            if (campfire.get().distSqr(playerPos) < 64 &&
                    player.blockPosition().distSqr(bedPos) < 10000) {
                return true;
            }
        }
        return false;
    }

    void commands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    void start(ServerAboutToStartEvent event) {
    }


    void reload(ServerAboutToStartEvent event) {
    }

      <F> void registerAll(Class<?> clazz, Registry<F> registry, Class<? extends F> filter) {
        Map<String,F> map = new HashMap<>();
          ((MappedRegistry<F>)registry).unfreeze();
        for (Field field : clazz.getFields()) {
            try {
                Object o = field.get(null);
                if (filter.isInstance(o)) {
                    map.put(field.getName().toLowerCase(Locale.ROOT),(F)o);
                }
            } catch (IllegalAccessException illegalAccessException) {
                illegalAccessException.printStackTrace();
            }
        }
        registerAll(map,registry);
    }

    public <F> void registerAll(Map<String, ? extends F> map, Registry<F> registry) {
        List<Pair<ResourceLocation, Supplier<?>>> list = registerLater.computeIfAbsent(registry, k -> new ArrayList<>());
        for (Map.Entry<String, ? extends F> entry : map.entrySet()) {
            list.add(Pair.of(id(entry.getKey()), entry::getValue));
        }
    }

    public static<T extends AbstractCriterionTriggerInstance> void
    checkQuestCriterion(SimpleCriterionTrigger<T>trigger,ServerPlayer pPlayer, Predicate<T> pTestTrigger) {
        CampfireLevelData campfireLevelData = CampfireLevelData.getOrCreate(pPlayer.server.overworld());
        if (campfireLevelData != null) {
            campfireLevelData.checkQuests(trigger,pPlayer,pTestTrigger);
        }
    }

    private void setup(final FMLCommonSetupEvent event) {
        PacketHandler.registerPackets();
    }

    void addBlocks(BlockEntityType<?> type,Block... blocks) {
        Set<Block> set = ((BlockEntityTypeAccessor)type).getValidBlocks();
        if (set instanceof HashSet<Block>) {

        } else {
            set = new HashSet<>(set);
            ((BlockEntityTypeAccessor)type).setValidBlocks(set);
        }
        Collections.addAll(set, blocks);
    }

    void register(RegisterEvent event){
        for (Map.Entry<Registry<?>,List<Pair<ResourceLocation, Supplier<?>>>> entry : registerLater.entrySet()) {
            Registry<?> registry = entry.getKey();
            List<Pair<ResourceLocation, Supplier<?>>> toRegister = entry.getValue();
            for (Pair<ResourceLocation,Supplier<?>> pair : toRegister) {
                event.register((ResourceKey<? extends Registry<Object>>)registry.key(),pair.getLeft(),(Supplier<Object>)pair.getValue());
            }
        }
        event.register(Registry.POINT_OF_INTEREST_TYPE_REGISTRY,id("town_campfire"),()-> ModPOIs.TOWN_CAMPFIRE);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID,path);
    }
}
