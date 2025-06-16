package tfar.towncampfires;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import tfar.towncampfires.config.TownCampfireConfig;

import java.util.*;

public class CampfireLevelData extends SavedData {

    private final ServerLevel level;
    private List<TownCampfire> campfiresByIndex = new ArrayList<>();
    private transient Map<BlockPos, TownCampfire> campfiresByPos = new HashMap<>();

    private Map<UUID,TownCampfire> lastVisited = new HashMap<>();

    public CampfireLevelData(ServerLevel pLevel) {
        this.level = pLevel;
    }

    public List<TownCampfire> getCampfiresByIndex() {
        return campfiresByIndex;
    }

    @Nullable
    public TownCampfire byLocation(BlockPos location) {
        return campfiresByPos.get(location);
    }

    public TownCampfire byIndex(int index){
        return campfiresByIndex.get(index);
    }

    public void save(TownCampfireBlockEntity be) {
        BlockPos blockPos = be.getBlockPos();
        if (!campfiresByPos.containsKey(blockPos)) {
            TownCampfire townCampfire = new TownCampfire(blockPos,
                    Util.getRandom(TownCampfireConfig.CONFIG.defaultNames.get(),be.getLevel().random),
                    0,0,0,new ArrayList<>(),TownCampfireConfig.CONFIG.starting_quests
                    .get().pick(be.getLevel().random));

            townCampfire.refresh(level);

            be.setLinkedCampfire(townCampfire);

            campfiresByIndex.add(townCampfire);
            campfiresByPos.put(blockPos, townCampfire);
            setDirty();
        } else {
            be.setLinkedCampfire(campfiresByPos.get(be.getBlockPos()));
        }
    }

    public void markVisit(ServerPlayer player,TownCampfire campfire) {
        lastVisited.put(player.getUUID(),campfire);
        setDirty();
    }

    public TownCampfire getLastVisited(UUID uuid) {
        return lastVisited.get(uuid);
    }

    public static CampfireLevelData getOrCreate(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(compoundTag -> loadStatic(compoundTag,level), () -> new CampfireLevelData(level),TownCampfires.MODID);
    }

    static CampfireLevelData loadStatic(CompoundTag compoundTag, ServerLevel level) {
        CampfireLevelData data = new CampfireLevelData(level);
        data.load(compoundTag);
        return data;
    }

    protected void load(CompoundTag compoundTag) {
        if (compoundTag.contains("campfires")) {
            ListTag campfiresTag = compoundTag.getList("campfires",Tag.TAG_COMPOUND);

            for (Tag tag : campfiresTag) {
                TownCampfire townCampfire = TownCampfire.load((CompoundTag) tag,this);
                campfiresByIndex.add(townCampfire);
                campfiresByPos.put(townCampfire.location(), townCampfire);
            }
        }
        if (compoundTag.contains("last_visited")) {
            CompoundTag tag = compoundTag.getCompound("last_visited");
            for (String key : tag.getAllKeys()) {
                int[] ints = tag.getIntArray(key);
                lastVisited.put(UUID.fromString(key),campfiresByPos.get(new BlockPos(ints[0],ints[1],ints[2])));
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag) {
        ListTag listTag = new ListTag();

        for (TownCampfire campfire : campfiresByIndex) {
            listTag.add(campfire.save());
        }
        pCompoundTag.put("campfires",listTag);


        CompoundTag tag = new CompoundTag();
        for (Map.Entry<UUID,TownCampfire> entry: lastVisited.entrySet()) {
            BlockPos pos = entry.getValue().location();
            tag.putIntArray(entry.getKey().toString(),new int[]{pos.getX(),pos.getY(),pos.getZ()});
        }
        pCompoundTag.put("last_visited",tag);


        return pCompoundTag;
    }

    public void tick() {
        for (TownCampfire campfire : campfiresByIndex) {
            campfire.update(level);
        }
    }
}
