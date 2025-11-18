package tfar.towncampfires.compat;

import fuzs.tradingpost.world.entity.npc.MerchantCollection;
import fuzs.tradingpost.world.inventory.TradingPostMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;

public class CampfireTradingMenu extends TradingPostMenu {
    public CampfireTradingMenu(int containerId, Inventory inventory) {
        super(containerId, inventory);
    }

    public CampfireTradingMenu(int containerId, Inventory inventory, MerchantCollection merchantCollection, ContainerLevelAccess worldPosCallable) {
        super(containerId, inventory, merchantCollection, worldPosCallable);
    }

    @Override
    public boolean stillValid(Player player) {
        return true||super.stillValid(player);
    }
}
