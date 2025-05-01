package tfar.towncampfires;

import com.mojang.serialization.Dynamic;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import tfar.towncampfires.config.TownCampfireConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CampfireLevelData extends SavedData {

    private final ServerLevel level;
    private List<TownCampfire> campfiresByIndex = new ArrayList<>();
    private transient Map<BlockPos, TownCampfire> campfiresByPos = new HashMap<>();

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
                    Util.getRandom(TownCampfireConfig.CONFIG.defaultNames.get(),be.getLevel().random),0,0,0);

            be.setLinkedCampfire(townCampfire);

            campfiresByIndex.add(townCampfire);
            campfiresByPos.put(blockPos, townCampfire);
            setDirty();
        } else {
            be.setLinkedCampfire(campfiresByPos.get(be.getBlockPos()));
        }
    }


    public static CampfireLevelData getOrCreate(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(compoundTag -> loadStatic(compoundTag,level), () -> new CampfireLevelData(level),TownCampfires.MODID);
    }

    static CampfireLevelData loadStatic(CompoundTag compoundTag, ServerLevel level) {
        CampfireLevelData dankSavedData = new CampfireLevelData(level);
        dankSavedData.load(compoundTag);
        return dankSavedData;
    }

    protected void load(CompoundTag compoundTag) {
        if (compoundTag.contains("campfires")) {
            Tag campfiresTag = compoundTag.get("campfires");
            campfiresByIndex = new ArrayList<>(TownCampfire.CODEC.listOf()
                    .parse(new Dynamic<>(NbtOps.INSTANCE, campfiresTag)).resultOrPartial(TownCampfires.LOGGER::error).orElse(new ArrayList<>()));

            for (TownCampfire townCampfire : campfiresByIndex) {
                campfiresByPos.put(townCampfire.location(), townCampfire);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag) {
        Tag campfiresTag = TownCampfire.CODEC.listOf().encodeStart(NbtOps.INSTANCE,campfiresByIndex).resultOrPartial(TownCampfires.LOGGER::error).orElseThrow();
        pCompoundTag.put("campfires",campfiresTag);
        return pCompoundTag;
    }

    public void tick() {
        for (TownCampfire campfire : campfiresByIndex) {
            campfire.update(level);
        }
    }
}
