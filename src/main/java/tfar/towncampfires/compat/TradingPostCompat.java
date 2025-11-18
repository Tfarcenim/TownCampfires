package tfar.towncampfires.compat;

import fuzs.tradingpost.init.ModRegistry;
import fuzs.tradingpost.mixin.accessor.VillagerAccessor;
import fuzs.tradingpost.world.entity.npc.MerchantCollection;
import fuzs.tradingpost.world.level.block.TradingPostBlock;
import fuzs.tradingpost.world.level.block.entity.TradingPostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Nameable;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import tfar.towncampfires.TownCampfire;

import java.util.Iterator;
import java.util.List;

public class TradingPostCompat {
    public static void openTradingPost(ServerPlayer player, Level level, BlockPos pos, TownCampfire campfire) {
                List<Entity> nearbyTraders = level.getEntitiesOfClass(Entity.class,campfire.getBoundingBox(), TradingPostCompat::canTrade);
                if (!nearbyTraders.isEmpty()) {
                    ContainerLevelAccess access = ContainerLevelAccess.create(level, pos);
                    MerchantCollection merchants = new MerchantCollection(access);

                    Entity merchant;
                    for(Iterator<Entity> var13 = nearbyTraders.iterator(); var13.hasNext(); merchants.addMerchant(merchant.getId(), (Merchant)merchant)) {
                        merchant = var13.next();
                        if (merchant instanceof Villager) {
                            ((VillagerAccessor)merchant).callUpdateSpecialPrices(player);
                        }
                    }

                    merchants.setTradingPlayer(player);
                    merchants.buildOffers(merchants.getIdToOfferCountMap());
                    Component title = getContainerTitle(level, pos);
                    openTradingScreen(player, merchants, title, access);
                } else {
                    player.displayClientMessage(TradingPostBlock.NO_MERCHANT_FOUND, false);
                }
            }
    private static Component getContainerTitle(Level level, BlockPos pos) {
        BlockEntity tileentity = level.getBlockEntity(pos);
        return tileentity instanceof TradingPostBlockEntity ? ((Nameable)tileentity).getDisplayName() : TradingPostBlock.CONTAINER_TITLE;
    }

    private static void openTradingScreen(Player player, MerchantCollection merchants, Component title, ContainerLevelAccess worldPosCallable) {
        player.openMenu(new SimpleMenuProvider((containerMenuId, playerInventory, playerEntity) -> {
            return new CampfireTradingMenu(containerMenuId, playerInventory, merchants, worldPosCallable);
        }, title)).ifPresent((containerId) -> {
            merchants.sendMerchantData(containerId, player);
        });
    }

    private static boolean canTrade(Entity entity) {
        if (!entity.getType().is(ModRegistry.BLACKLISTED_TRADERS_TAG)) {
            if (entity.isAlive() && entity instanceof Merchant && ((Merchant)entity).getTradingPlayer() == null && !((Merchant)entity).getOffers().isEmpty()) {
                return !(entity instanceof LivingEntity) || !((LivingEntity)entity).isSleeping() && !((LivingEntity)entity).isBaby();
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
