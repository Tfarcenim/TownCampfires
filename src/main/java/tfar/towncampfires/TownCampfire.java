package tfar.towncampfires;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import tfar.towncampfires.config.RandomIntegerRange;
import tfar.towncampfires.config.TownCampfireConfig;
import tfar.towncampfires.data.CampfireEffect;
import tfar.towncampfires.network.ForgePacketHandler;
import tfar.towncampfires.network.client.S2CTownCampfirePacket;
import tfar.towncampfires.utils.MiscCodecs;
import tfar.towncampfires.utils.Utils;

import javax.annotation.Nullable;
import java.util.*;

public final class TownCampfire {
    public static final Codec<TownCampfire> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    BlockPos.CODEC.fieldOf("location").forGetter(TownCampfire::location),
                    MiscCodecs.COMPONENT_CODEC.fieldOf("name").forGetter(TownCampfire::name),
                    Codec.LONG.fieldOf("experience").forGetter(TownCampfire::getExperience),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("used_blocks").forGetter(TownCampfire::getUsedBlocks),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("used_workbenches").forGetter(TownCampfire::getUsedBlocks),
                    ResourceLocation.CODEC.listOf().fieldOf("visible_quests").forGetter(TownCampfire::getQuestIds),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("max_quests").forGetter(campfire -> campfire.maxQuests)
                    ).apply(instance, TownCampfire::new));
    private final BlockPos location;
    private Component name;
    private long experience;
    private transient int currentVillagers;
    private int usedBlocks;
    private int usedWorkbenches;

    transient RandomSource random = RandomSource.createNewThreadLocalInstance();

    private final List<ResourceLocation> effects = new ArrayList<>();
    private List<ResourceLocation> quests;

    @Nullable
    private transient ResourceLocation levelQuest;

    private int maxQuests;


    boolean resync;

    public TownCampfire(BlockPos location, Component name, long experience, int usedBlocks, int usedWorkbenches, List<ResourceLocation> quests,int maxQuests) {
        this.location = location;
        this.name = name;
        this.experience = experience;
        this.usedBlocks = usedBlocks;
        this.usedWorkbenches = usedWorkbenches;
        this.quests = quests;
        this.maxQuests = maxQuests;
        removeInvalidEffects();
    }

    public TownCampfire constructForPlayer(ServerPlayer player, CampfireLevelData campfireLevelData) {

        List<ResourceLocation> quests = new ArrayList<>(this.quests);

        quests.removeIf(resourceLocation -> campfireLevelData.completedQuests.getOrDefault(player.getUUID(),Set.of()).contains(resourceLocation));

        TownCampfire townCampfire = new TownCampfire(location,name,experience,usedBlocks,usedWorkbenches,quests, maxQuests);

        return townCampfire;
    }

    public static TownCampfire load(CompoundTag tag,CampfireLevelData data) {
        TownCampfire townCampfire = TownCampfire.CODEC
                .parse(new Dynamic<>(NbtOps.INSTANCE, tag)).resultOrPartial(TownCampfires.LOGGER::error).orElseThrow();


        ListTag activeEffects = tag.getList("active_effects",Tag.TAG_STRING);
        List<ResourceLocation> effe = new ArrayList<>();

        for (Tag t : activeEffects) {
            effe.add(new ResourceLocation(t.getAsString()));
        }

        townCampfire.effects.addAll(effe);

        return townCampfire;
    }

    public CompoundTag save() {
        CompoundTag tag = (CompoundTag) TownCampfire.CODEC.encodeStart(NbtOps.INSTANCE,this).resultOrPartial(TownCampfires.LOGGER::error).orElseThrow();
        return tag;
    }

    public static TownCampfire fromPacket(BlockPos location, Component name, long experience, int currentVillagers, int usedBlocks, int usedWorkbenches,
                                          List<ResourceLocation> effects, List<ResourceLocation> quests, int startingQuestCount) {
        TownCampfire townCampfire = new TownCampfire(location, name,experience,usedBlocks,usedWorkbenches,quests,startingQuestCount);
        townCampfire.currentVillagers = currentVillagers;
        townCampfire.effects.addAll(effects);
        return townCampfire;
    }

    public void toPacket(FriendlyByteBuf buf) {
        buf.writeBlockPos(location);
        buf.writeComponent(name);
        buf.writeLong(experience);
        buf.writeInt(currentVillagers);
        buf.writeInt(usedBlocks);
        buf.writeInt(usedWorkbenches);

        buf.writeCollection(effects,FriendlyByteBuf::writeResourceLocation);
        buf.writeCollection(quests,FriendlyByteBuf::writeResourceLocation);
        buf.writeInt(maxQuests);
    }

    public static TownCampfire fromPacket(FriendlyByteBuf buf) {
        BlockPos location = buf.readBlockPos();
        Component name = buf.readComponent();
        long experience = buf.readLong();
        int currentVillagers = buf.readInt();
        int usedBlocks = buf.readInt();
        int usedWorkbenches = buf.readInt();

        List<ResourceLocation> resourceLocations = buf.readList(FriendlyByteBuf::readResourceLocation);
        List<ResourceLocation> questIds = buf.readList(FriendlyByteBuf::readResourceLocation);

        int startingQuestCount = buf.readInt();

        return fromPacket(location, name,experience,currentVillagers,usedBlocks,usedWorkbenches,resourceLocations,questIds,startingQuestCount);
    }


    void removeInvalidEffects() {
        effects.removeIf(location1 -> !TownCampfires.campfireEffectLoader.getCampfireEffects().containsKey(location1));
    }

    public int getCurrentVillagers() {
        return currentVillagers;
    }

    public int getEffectiveVillagers() {
        return Math.min(getCurrentVillagers(),getMaxVillagers());
    }

    public int getMaxVillagers() {
        return TownCampfireConfig.CONFIG.base_villagers.get() + TownCampfireConfig.CONFIG.villagers_per_level.get() * getLevel();
    }

    public List<ResourceLocation> getEffectIds() {
        return effects;
    }

    public List<ResourceLocation> getQuestIds() {
        return quests;
    }

    public int getUsedWorkbenches() {
        return usedWorkbenches;
    }

    public boolean hasFreeWorkbenches() {
        return usedWorkbenches < getAllowedWorkbenches();
    }

    public void incrementUsedWorkbenches() {
        usedWorkbenches++;
    }

    public int getAllowedWorkbenches() {
        return TownCampfireConfig.CONFIG.base_allowed_workbenches.get() + TownCampfireConfig.CONFIG.allowed_workbenches_per_level.get() * getLevel();
    }

    public int getUsedBlocks() {
        return usedBlocks;
    }

    public boolean hasFreeBlocks() {
        return usedBlocks < getAllowedBlocks();
    }

    public void incrementUsedBlocks() {
        usedBlocks++;
    }

    public int getAllowedBlocks() {
        return TownCampfireConfig.CONFIG.base_allowed_blocks.get() + TownCampfireConfig.CONFIG.allowed_blocks_per_level.get() * getLevel();
    }

    public void setMaxQuests(int maxQuests) {
        this.maxQuests = maxQuests;
    }

    public int getMaxQuests() {
        return maxQuests;
    }

    public int getAvailableQuests() {
        return quests.size();
    }

    public double getRadius() {
        return TownCampfireConfig.CONFIG.radius.get();
    }

    public void refresh(ServerLevel level) {
        usedBlocks = 0;
        usedWorkbenches = 0;
        levelQuest = null;

        if (isLoaded(level)) {
            sampleNearbyBiomes(level);
        }

        rollEffects(level);
        sampleQuests(level);

        resync = true;
    }

    private transient HolderSet<Biome> nearbyBiomes = HolderSet.direct();

    void sampleNearbyBiomes(ServerLevel level) {
        int search = 100;
        Set<Holder<Biome>> sampled = new HashSet<>();
        for (int z = - search ; z < search;z++) {
            for (int x = - search ; x < search;x++) {
                sampled.add(level.getBiome(location.offset(x,0,z)));
            }
        }
        nearbyBiomes = HolderSet.direct(sampled.stream().toList());
    }

    public HolderSet<Biome> getNearbyBiomes() {
        return nearbyBiomes;
    }

    void rollEffects(ServerLevel level) {
        effects.clear();

        sampleEffects(level,TownCampfireConfig.CONFIG.positive_effects.get(),MobEffectCategory.BENEFICIAL);
        sampleEffects(level,TownCampfireConfig.CONFIG.negative_effects.get(),MobEffectCategory.HARMFUL);
    }

    void sampleEffects(ServerLevel level,List<RandomIntegerRange> randomIntegerRanges ,MobEffectCategory category) {
        if (!randomIntegerRanges.isEmpty()) {
            Holder<Biome> biome = level.getBiome(location);
            List<ResourceLocation> possibleEffects = TownCampfires.campfireEffectLoader.getEligibleEffects(getLevel(),category,biome,nearbyBiomes);
            Collections.shuffle(possibleEffects);
            RandomIntegerRange range = randomIntegerRanges.get(Math.min(getLevel(), randomIntegerRanges.size() - 1));
            int effectCount = Math.min(range.roll(random),possibleEffects.size());
            effects.addAll(Utils.pickEffects(level.random,possibleEffects,effectCount));
        }
    }

    void  sampleQuests(ServerLevel level) {
        quests = new ArrayList<>();
        List<ResourceLocation> possibleQuests = TownCampfires.questLoader.getEligibleQuests(this,level);

        quests.addAll(possibleQuests);
    }

    AABB aabb;

    public AABB getBoundingBox() {
        if (aabb == null) {
            aabb = new AABB(location).inflate(getRadius());
        }
        return aabb;
    }

    public void setCurrentVillagers(int currentVillagers) {
        resync |= this.currentVillagers != currentVillagers;
        this.currentVillagers = currentVillagers;
    }

    public BlockPos location() {
        return location;
    }

    public Component name() {
        return name;
    }

    public void setName(Component name) {
        this.name = name;
    }

    public long getExperience() {
        return experience;
    }

    public void setExperience(long experience) {
        resync |= this.experience != experience;
        this.experience = experience;
    }

    public void addExperience(long reward) {
        setExperience(experience + reward);
    }

    public int getLevel() {
        return calculateLevel(experience);
    }

    public boolean atLevelThreshold() {
        int level = getLevel();
        List<? extends Integer> integers = TownCampfireConfig.CONFIG.levelup_walls.get();
        for (Integer i : integers) {
            if (i - 1 == level) {
                return true;
            }
        }
        return false;
    }

    public static long timeUntilRefresh(long gameTime) {
        long timer = TownCampfireConfig.CONFIG.refresh_timer.get();
        long modulo = gameTime % timer;

        return timer - modulo;
    }

    public static int calculateLevel(long experience) {
        return (int) (experience / TownCampfireConfig.CONFIG.experience_per_level.get());
    }

    public boolean isLoaded(Level level) {
        return level.isAreaLoaded(location, 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TownCampfire) obj;
        return Objects.equals(this.location, that.location) &&
                Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, name);
    }

    @Override
    public String toString() {
        return "TownCampfire[" +
                "location=" + location + ", " +
                "name=" + name + ']';
    }


    public void update(ServerLevel pLevel,boolean refresh) {
        double radius = getRadius();
        boolean loaded = isLoaded(pLevel);

        if (pLevel.getGameTime() % 80 == 0 && loaded) {
            AABB aabb = new AABB(location).inflate(radius).expandTowards(0, pLevel.getHeight(), 0);
            List<Player> list = pLevel.getEntitiesOfClass(Player.class, aabb);
            for(Player player : list) {
                for (ResourceLocation instance : effects) {
                    CampfireEffect campfireEffect = TownCampfires.campfireEffectLoader.getCampfireEffects().get(instance);
                    if (campfireEffect != null) {
                        MobEffectInstance mobEffectInstance = new MobEffectInstance(campfireEffect.effect().getEffect());
                        mobEffectInstance.update(campfireEffect.effect());
                        player.addEffect(mobEffectInstance);
                    }
                }
            }
        }

        if(refresh) {
            refresh(pLevel);
        }

        if (atLevelThreshold()) {
            if (levelQuest == null) {
                levelQuest = TownCampfires.questLoader.addLevelQuest(this,pLevel);
                if (levelQuest != null) {
                    quests.add(0,levelQuest);
                    resync = true;
                }
            }
        }

        if (pLevel.getGameTime() % 20 == 0 && loaded) {
            List<Villager> villagers = pLevel.getEntitiesOfClass(Villager.class, getBoundingBox());
            setCurrentVillagers(villagers.size());
            if (resync) {
                List<Player> players = pLevel.getEntitiesOfClass(Player.class,new AABB(location).inflate(8));
                for (Player player : players) {
                    ForgePacketHandler.sendToClient(new S2CTownCampfirePacket(this),(ServerPlayer)player);
                }
                resync = false;
            }
        }
    }

    public void giveExperiencePoints(int campfireExperience, boolean isLevelup) {

        int currentLevel = calculateLevel(experience);
        int predictedLevel = calculateLevel(campfireExperience+experience);


        if (predictedLevel > currentLevel && !isLevelup) {
            boolean problematic = false;
            for (int i = currentLevel+1;i <=predictedLevel;i++) {
                if (TownCampfireConfig.CONFIG.levelup_walls.get().contains(i)) {
                    problematic = true;
                    break;
                }
            }

            if (problematic) {
                int remaining = campfireExperience;
                while (remaining > 0) {
                    int predict = calculateLevel(experience+1);
                    if (TownCampfireConfig.CONFIG.levelup_walls.get().contains(predict)) {
                        break;
                    } else {
                        experience++;
                        remaining--;
                        resync = true;
                    }
                }
            } else {
                setExperience(experience + campfireExperience);
            }
        } else {
            setExperience(experience + campfireExperience);
        }

        if (isLevelup) {
            maxQuests++;
        }
    }
}
