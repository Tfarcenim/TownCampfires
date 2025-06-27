package tfar.towncampfires.client;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.data.CampfireEffectLoader;
import tfar.towncampfires.data.quest.Quest;
import tfar.towncampfires.data.quest.QuestCriteria;
import tfar.towncampfires.data.quest.QuestInstance;
import tfar.towncampfires.data.quest.QuestLoader;
import tfar.towncampfires.init.ModBlocks;
import tfar.towncampfires.network.client.S2CQuestPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TownCampfiresClient {

    public static CampfireEffectLoader campfireEffectLoader = new CampfireEffectLoader();
    public static QuestLoader questLoader = new QuestLoader(null);

    public static Map<Quest, ResourceLocation> reverseLookup;

    static List<QuestInstance> currentQuests = new ArrayList<>();

    public static void init(IEventBus bus) {
        bus.addListener(TownCampfiresClient::setup);
        bus.addListener(TownCampfiresClient::overlays);
    }

    static void overlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("quests",overlay);
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

    public static void handleSync(TownCampfire townCampfire) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof TownCampfireScreen townCampfireScreen) {
            townCampfireScreen.setCampfire(townCampfire);
        }
    }

    static IGuiOverlay overlay = (gui, poseStack, partialTick, screenWidth, screenHeight) -> {
        int startY = 10;
        Font font = gui.getFont();
        for (int i = 0; i < currentQuests.size();i++) {
            QuestInstance questInstance = currentQuests.get(i);
            Quest quest = questInstance.quest();
            font.draw(poseStack,quest.name(),5,startY+i * 20,0xffffff);
            for (int j = 0 ; j < quest.criterias().size();j++) {
                Pair<QuestCriteria<?>, Integer> questCriteriaIntegerPair = quest.criterias().get(j);
                int needed= questCriteriaIntegerPair.getSecond();
                Integer progress = questInstance.progress().isEmpty() ? 0 : questInstance.progress().get(j);
                font.draw(poseStack,questCriteriaIntegerPair.getFirst().desc().copy().append(" "+progress+"/"+needed),5, startY + i * 20 + 10, 0xffffff);
            }
        }
    };

    public static void setQuests(List<QuestInstance> questInstanceList) {
        currentQuests = questInstanceList;
        if (Minecraft.getInstance().screen instanceof TownCampfireScreen townCampfireScreen) {
            townCampfireScreen.updateQuests();
        }
    }

    public static void handle(S2CQuestPacket s2CQuestPacket) {
        Map<ResourceLocation, Quest> effects = s2CQuestPacket.effects();
        questLoader.setFromServer(effects);
    }

    public static ResourceLocation clientLookup(Quest quest) {
        return questLoader.getQuestMap().keySet().stream()
                .filter(e -> Objects.equals(questLoader.getQuestMap().get(e).name(), quest.name())).findFirst().orElse(null);
    }
}
