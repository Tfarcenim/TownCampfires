package tfar.towncampfires.client;

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
import tfar.towncampfires.config.TownCampfireConfig;
import tfar.towncampfires.data.CampfireEffectLoader;
import tfar.towncampfires.data.quest.Quest;
import tfar.towncampfires.data.quest.QuestCriteria;
import tfar.towncampfires.data.quest.QuestInstance;
import tfar.towncampfires.data.quest.QuestLoader;
import tfar.towncampfires.init.ModBlocks;
import tfar.towncampfires.network.client.S2CQuestAttemptPacket;
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

    static Map<ResourceLocation,Integer> attempts;

    public static void init(IEventBus bus) {
        bus.addListener(TownCampfiresClient::setup);
        bus.addListener(TownCampfiresClient::overlays);
    }

    public static int attempts(ResourceLocation resourceLocation) {
        return attempts.getOrDefault(resourceLocation,0);
    }

    public static int getUsedSlots() {
        int count = 0;
        for (QuestInstance instance : currentQuests) {
            count+=instance.quest().slots();
        }
        return count;
    }

    public static int getFreeSlots() {
        return TownCampfireConfig.CONFIG.slots_per_player.get() - getUsedSlots();
    }

    public static boolean hasEnoughSlots(Quest quest) {
        return quest.slots()+getUsedSlots() <= TownCampfireConfig.CONFIG.slots_per_player.get();
    }

    public static boolean canAttempt(ResourceLocation questID) {
        Quest quest = questLoader.getQuestMap().get(questID);
        return attempts(questID) < quest.attempts();
    }

    public static boolean isQuestAlreadyActive(ResourceLocation resourceLocation) {
        for (QuestInstance questInstance : currentQuests) {
            if (Objects.equals(resourceLocation,questInstance.questID())) {
                return true;
            }
        }
        return false;
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

            font.draw(poseStack,quest.name().copy().append(questInstance.status() == QuestInstance.Status.PREP ? " (Prep)":""),5,startY+i * 20,0xffffff);
            for (int j = 0; j < quest.successCriteria().custom().size(); j++) {
                QuestCriteria<?> questCriteria = quest.successCriteria().custom().get(j);
                int needed= questCriteria.count();
                Integer progress = questInstance.customProgress().isEmpty() ? 0 : questInstance.customProgress().get(j);
                font.draw(poseStack,questCriteria.desc().copy().append(" "+progress+"/"+needed),5, startY + i * 20 + 10, 0xffffff);
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

    public static void handle(S2CQuestAttemptPacket s2CQuestAttemptPacket) {
        attempts = s2CQuestAttemptPacket.attempts();
    }
}
