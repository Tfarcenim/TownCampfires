package tfar.towncampfires.network.server;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import tfar.towncampfires.CampfireLevelData;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.compat.LoadedMods;
import tfar.towncampfires.compat.TradingPostCompat;

import java.util.Optional;

public class C2STownCampfireButtonPacket implements C2SModPacket {

    final CampfireButton campfireButton;
    final BlockPos pos;

    public C2STownCampfireButtonPacket(FriendlyByteBuf buf) {
        campfireButton = buf.readEnum(CampfireButton.class);
        pos = buf.readBlockPos();
    }

    public C2STownCampfireButtonPacket(CampfireButton campfireButton, BlockPos pos) {
        this.campfireButton = campfireButton;
        this.pos = pos;
    }

    @Override
    public void handleServer(ServerPlayer player) {
        CampfireLevelData campfireLevelData = CampfireLevelData.getOrCreate(player.getLevel());
        TownCampfire townCampfire = campfireLevelData.byLocation(pos);
        if (townCampfire != null) {
            switch (campfireButton) {
                case SPAWN -> player.setRespawnPosition(player.getLevel().dimension(), player.blockPosition(), 0, true, false);
                case BED -> {
                    Optional<BlockPos> optional =  player.getLevel().getPoiManager().getRandom(holder -> holder.is(PoiTypes.HOME), pos -> true,
                            PoiManager.Occupancy.ANY, player.blockPosition(), (int) townCampfire.getRadius(), player.getRandom());
                    optional.ifPresent(pos1 -> player.startSleepInBed(pos1).ifLeft(problem -> {
                        if (problem.getMessage() != null) {
                            player.displayClientMessage(problem.getMessage(), true);
                        }
                    }));
                }
                case TRADE -> {
                    if (LoadedMods.tradingpost.loaded) {
                        TradingPostCompat.openTradingPost(player,player.getLevel(),pos,townCampfire);
                    } else {
                        player.displayClientMessage(Component.literal("Install Trading Post to use this tab"),false);
                    }
                }
            }
        } else {
            TownCampfires.LOGGER.warn("Player {} attempted to access nonexistent campfire at {}",player,pos);
        }
    }

    @Override
    public void write(FriendlyByteBuf to) {
        to.writeEnum(campfireButton);
        to.writeBlockPos(pos);
    }

    public enum CampfireButton {
        SPAWN,BED,TRADE
    }

}
