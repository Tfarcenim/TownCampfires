package tfar.towncampfires.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import tfar.towncampfires.init.ModBlocks;

public class TownCampfiresClient {

    public static void init(IEventBus bus) {
        bus.addListener(TownCampfiresClient::setup);
    }

    @SuppressWarnings("removal")
    static void setup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.ORANGE_TOWN_CAMPFIRE, RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.RED_TOWN_CAMPFIRE, RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIGHT_BLUE_TOWN_CAMPFIRE, RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.GREEN_TOWN_CAMPFIRE, RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.GRAY_TOWN_CAMPFIRE, RenderType.cutout());
    }

    public static void openCampfireScreen(Player pPlayer) {
        Minecraft.getInstance().setScreen(new TownCampfireScreen(Component.empty()));
    }
}
