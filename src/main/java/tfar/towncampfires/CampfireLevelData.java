package tfar.towncampfires;

import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CampfireLevelData extends SavedData {

    private final ServerLevel level;
    private List<CampfireInfo> campfiresByIndex = new ArrayList<>();
    private transient Map<BlockPos,CampfireInfo> campfiresByPos = new HashMap<>();

    public CampfireLevelData(ServerLevel pLevel) {
        this.level = pLevel;
    }

    public List<CampfireInfo> getCampfiresByIndex() {
        return campfiresByIndex;
    }

    @Nullable
    public CampfireInfo byLocation(BlockPos location) {
        return campfiresByPos.get(location);
    }

    public CampfireInfo byIndex(int index){
        return campfiresByIndex.get(index);
    }

    public void save(CampfireBlockEntity be) {
        BlockPos blockPos = be.getBlockPos();
        if (!campfiresByPos.containsKey(blockPos)) {
            CampfireInfo campfireInfo = new CampfireInfo(blockPos);

            campfiresByIndex.add(campfireInfo);
            campfiresByPos.put(blockPos,campfireInfo);
            setDirty();
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
        Tag campfiresTag = compoundTag.getCompound("campfires");
        campfiresByIndex = new ArrayList<>(CampfireInfo.CODEC.listOf()
                .parse(new Dynamic<>(NbtOps.INSTANCE,campfiresTag)).resultOrPartial(TownCampfires.LOGGER::error).orElseThrow());

        for (CampfireInfo campfireInfo : campfiresByIndex) {
            campfiresByPos.put(campfireInfo.location(),campfireInfo);
        }
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag) {
        Tag campfiresTag = CampfireInfo.CODEC.listOf().encodeStart(NbtOps.INSTANCE,campfiresByIndex).resultOrPartial(TownCampfires.LOGGER::error).orElseThrow();
        pCompoundTag.put("campfires",campfiresTag);
        return pCompoundTag;
    }
}
