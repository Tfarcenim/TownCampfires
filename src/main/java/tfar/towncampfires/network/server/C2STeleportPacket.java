package tfar.towncampfires.network.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import tfar.towncampfires.CampfireLevelData;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.config.TownCampfireConfig;

public record C2STeleportPacket(BlockPos destination) implements C2SModPacket {

    @Override
    public void handleServer(ServerPlayer player) {
        CampfireLevelData campfireLevelData = CampfireLevelData.getOrCreate(player.getLevel());
        TownCampfire townCampfire = campfireLevelData.byLocation(destination);
        if (townCampfire!=null) {
            double dist = Math.sqrt(townCampfire.location().distSqr(destination));

            if (dist > TownCampfireConfig.CONFIG.unteleportable_distance.get())return;

            int cost = (int) Math.ceil(Mth.clamp(dist/TownCampfireConfig.CONFIG.teleport_distance_per_item_cost.get(),TownCampfireConfig.CONFIG.teleport_minimum_cost.get()
                    ,TownCampfireConfig.CONFIG.teleport_maximum_cost.get()));
            switch (TownCampfireConfig.CONFIG.teleport_requirement.get()) {
                case ITEM -> {
                    int itemCount = player.getInventory().clearOrCountMatchingItems(itemStack ->
                            itemStack.is(Registry.ITEM.get(new ResourceLocation(TownCampfireConfig.CONFIG.teleport_item.get())
                            )), 0, player.inventoryMenu.getCraftSlots());

                    if (itemCount >= cost){
                        player.getInventory().clearOrCountMatchingItems(itemStack ->
                                itemStack.is(Registry.ITEM.get(new ResourceLocation(TownCampfireConfig.CONFIG.teleport_item.get())
                                )), cost, player.inventoryMenu.getCraftSlots());
                    }
                }
                case EXPERIENCE -> {
                    if (player.totalExperience >=cost){
                        player.giveExperiencePoints(-cost);
                        player.teleportTo(destination.getX()+1.5,destination.getY(),destination.getZ()+.5);
                    }
                }
            }
        }
    }

    public static C2STeleportPacket fromPacket(FriendlyByteBuf buf){
        return new C2STeleportPacket(buf.readBlockPos());
    }

    @Override
    public void write(FriendlyByteBuf to) {
        to.writeBlockPos(destination);
    }
}
