package tfar.towncampfires;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import tfar.towncampfires.network.ForgePacketHandler;
import tfar.towncampfires.network.client.S2CTownCampfirePacket;
import tfar.towncampfires.utils.MiscCodecs;
import tfar.towncampfires.utils.Utils;

import java.util.*;

public final class TownCampfire {
    public static final Codec<TownCampfire> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    BlockPos.CODEC.fieldOf("location").forGetter(TownCampfire::location),
                    MiscCodecs.COMPONENT_CODEC.fieldOf("name").forGetter(TownCampfire::name),
                    Codec.LONG.fieldOf("experience").forGetter(TownCampfire::getExperience),
                    Codec.INT.fieldOf("used_blocks").forGetter(TownCampfire::getUsedBlocks),
                    Codec.INT.fieldOf("used_workbenches").forGetter(TownCampfire::getUsedBlocks),
                    ResourceLocation.CODEC.listOf().fieldOf("active_effects").forGetter(TownCampfire::getEffectIds)
            ).apply(instance, TownCampfire::new));
    private final BlockPos location;
    private Component name;
    private long experience;
    private transient int currentVillagers;
    private int usedBlocks;
    private int usedWorkbenches;

    transient RandomSource random = RandomSource.createNewThreadLocalInstance();

    private List<ResourceLocation> effects;

    private transient List<CampfireEffect> cachedEffects;

    boolean resync;

    public TownCampfire(BlockPos location, Component name, long experience, int usedBlocks, int usedWorkbenches,List<ResourceLocation> effects) {
        this.location = location;
        this.name = name;
        this.experience = experience;
        this.usedBlocks = usedBlocks;
        this.usedWorkbenches = usedWorkbenches;
        this.effects = effects;
        removeInvalidEffects();
    }

    public static TownCampfire fromPacket(BlockPos location, Component name,long experience,int currentVillagers,int usedBlocks,int usedWorkbenches,List<ResourceLocation> effects) {
        TownCampfire townCampfire = new TownCampfire(location, name,experience,usedBlocks,usedWorkbenches,effects);
        townCampfire.currentVillagers = currentVillagers;
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
    }

    public static TownCampfire fromPacket(FriendlyByteBuf buf) {
        BlockPos location = buf.readBlockPos();
        Component name = buf.readComponent();
        long experience = buf.readLong();
        int currentVillagers = buf.readInt();
        int usedBlocks = buf.readInt();
        int usedWorkbenches = buf.readInt();

        List<ResourceLocation> resourceLocations = buf.readList(FriendlyByteBuf::readResourceLocation);

        return fromPacket(location, name,experience,currentVillagers,usedBlocks,usedWorkbenches,resourceLocations);
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

    public double getRadius() {
        return TownCampfireConfig.CONFIG.radius.get();
    }

    public void refresh(ServerLevel level) {
        usedBlocks = 0;
        usedWorkbenches = 0;

        if (isLoaded(level)) {
            sampleNearbyBiomes(level);
        }

        rollEffects(level);

        resync = true;
    }

    private HolderSet<Biome> nearbyBiomes = HolderSet.direct();

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

    void rollEffects(ServerLevel level) {
        cachedEffects = null;

        effects = new ArrayList<>();

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
        return (int) (experience / TownCampfireConfig.CONFIG.experience_per_level.get());
    }

    public static long timeUntilRefresh(long gameTime) {
        long timer = TownCampfireConfig.CONFIG.refresh_timer.get();
        long modulo = gameTime % timer;

        return timer - modulo;
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


    public void update(ServerLevel pLevel) {
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

        if (pLevel.getGameTime() % TownCampfireConfig.CONFIG.refresh_timer.get() == 0) {
            refresh(pLevel);
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
}
