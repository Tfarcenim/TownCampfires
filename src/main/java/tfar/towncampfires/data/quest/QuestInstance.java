package tfar.towncampfires.data.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.TownCampfires;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuestInstance {

    private final TownCampfire campfire;
    private final Quest quest;
    private boolean active;
    private final UUID leader;
    List<UUID> members = new ArrayList<>();

    public QuestInstance(TownCampfire campfire,Quest quest,UUID leader) {
        this.campfire = campfire;
        this.quest = quest;
        this.leader = leader;
    }

    public Quest quest() {
        return quest;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public boolean hasPlayer(ServerPlayer player) {
        return members.contains(player.getUUID());
    }

    public boolean isLeader(ServerPlayer player) {
        return player.getUUID().equals(leader);
    }

    public void addPlayer(ServerPlayer player) {
        addPlayer(player.getUUID());
    }

    public void addPlayer(UUID uuid) {
        members.add(uuid);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("quest",TownCampfires.questLoader.lookup(quest).toString());
        tag.putString("leader",leader.toString());
        ListTag listTag = new ListTag();
        for (UUID uuid : members) {
            listTag.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("members",listTag);
        return tag;
    }

    public static QuestInstance load(TownCampfire campfire,CompoundTag tag) {
        ResourceLocation questID = new ResourceLocation(tag.getString("quest"));
        Quest quest = TownCampfires.questLoader.getQuestMap().get(questID);
        UUID leader = UUID.fromString(tag.getString("leader"));
        QuestInstance questInstance = new QuestInstance(campfire,quest,leader);
        ListTag listTag = tag.getList("members", Tag.TAG_STRING);
        for (Tag tag1 : listTag) {
            questInstance.addPlayer(UUID.fromString(tag1.getAsString()));
        }
        return questInstance;
    }
}
