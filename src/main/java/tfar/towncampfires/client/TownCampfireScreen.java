package tfar.towncampfires.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import net.minecraftforge.common.UsernameCache;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.config.TownCampfireConfig;
import tfar.towncampfires.data.CampfireEffect;
import tfar.towncampfires.data.QuestLogEntry;
import tfar.towncampfires.data.quest.Quest;
import tfar.towncampfires.data.quest.QuestCriteria;
import tfar.towncampfires.data.quest.QuestInstance;
import tfar.towncampfires.data.quest.criteria.Delivery;
import tfar.towncampfires.network.ForgePacketHandler;
import tfar.towncampfires.network.server.C2SSetTownCampfireNamePacket;
import tfar.towncampfires.network.server.C2STeleportPacket;
import tfar.towncampfires.network.server.C2STownCampfireButtonPacket;
import tfar.towncampfires.network.server.C2STownCampfireQuestPacket;
import tfar.towncampfires.utils.TextComponents;
import tfar.towncampfires.utils.Utils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class TownCampfireScreen extends BasicScreen {

    protected EditBox name;
    protected EditBox teleportSearch;
    protected EditBox logSearch;
    @Nullable
    private List<FormattedCharSequence> toolTip;

    protected TownCampfire townCampfire;

    List<TownCampfire> teleports = new ArrayList<>();

    public void setTeleports(List<TownCampfire> campfires) {
        teleports = campfires;
        teleportWidget.refresh();
    }

    public void setCampfire(TownCampfire townCampfire) {
        this.townCampfire = townCampfire;
        name.setValue(townCampfire.name().getString());
        campfireEffectWidget.refreshList();
        updateQuests();
    }

    public void updateQuests() {
        questWidget.refreshList();
        activeQuestWidget.refreshList();
    }

    public enum Tab {
        status, quest, trades, teleport, logs
    }

    protected static final int TAB_HEIGHT = 20;

    protected Tab current = Tab.status;
    protected Button home;
    protected Button bed;
    protected Button gear;

    protected CampfireEffectWidget campfireEffectWidget;
    protected QuestWidget questWidget;
    protected ActiveQuestWidget activeQuestWidget;
    protected TeleportWidget teleportWidget;
    protected LogWidget logWidget;

    protected Button startQuest;

    protected Button finishQuest;

    protected Button questTabSwitch;
    protected Button info;
    protected Button deliver;

    protected Button teleport;

    protected Button teleportFilter;
    protected Button logFilter;

    protected TownCampfireScreen(Component pTitle) {
        super(pTitle);
    }

    @Override
    protected void init() {
        super.init();
        Tab[] values = Tab.values();
        int tabWidth = imageWidth / values.length;
        for (int i = 0; i < values.length; i++) {
            Tab tab = values[i];
            int xPos = leftPos + i * tabWidth;
            TabButton tabButton = new TabButton(xPos, topPos, tabWidth, TAB_HEIGHT + 4, Component.literal(tab.name()), pButton -> switchToTab(tab), tab);
            tabButton.setFGColor(DARK_GRAY);
            addRenderableWidget(tabButton);
        }

        int yPos = topPos + 55;

        int statusHeight = 100;

        campfireEffectWidget = new CampfireEffectWidget(minecraft, 160, statusHeight, yPos, yPos + statusHeight, 20);
        campfireEffectWidget.setLeftPos(leftPos + 150);
        campfireEffectWidget.setRenderTopAndBottom(false);
        campfireEffectWidget.setRenderBackground(false);
        this.addRenderableWidget(campfireEffectWidget);

        int questWidth = imageWidth / 2 - 10;

        int questHeight = 240;
        questWidget = new QuestWidget(minecraft, questWidth, questHeight, topPos + TAB_HEIGHT + 20, topPos + questHeight - 10, 50);
        questWidget.setLeftPos(leftPos + 5);
        questWidget.setRenderTopAndBottom(false);
        //campfireEffectWidget.setRenderBackground(false);
        this.addRenderableWidget(questWidget);

        activeQuestWidget = new ActiveQuestWidget(minecraft, questWidth, questHeight, topPos + TAB_HEIGHT + 20, topPos + questHeight - 10, 50);
        activeQuestWidget.setLeftPos(leftPos + 5);
        activeQuestWidget.setRenderTopAndBottom(false);
        //campfireEffectWidget.setRenderBackground(false);
        this.addRenderableWidget(activeQuestWidget);

        teleportWidget = new TeleportWidget(minecraft, questWidth, questHeight, topPos + TAB_HEIGHT + 20, topPos + questHeight - 10, 50);
        teleportWidget.setLeftPos(leftPos + 5);
        teleportWidget.setRenderTopAndBottom(false);
        //campfireEffectWidget.setRenderBackground(false);
        this.addRenderableWidget(teleportWidget);

        //
        logWidget = new LogWidget(minecraft, questWidth, questHeight, topPos + TAB_HEIGHT + 20, topPos + questHeight - 10, 50);
        logWidget.setLeftPos(leftPos + 5);
        logWidget.setRenderTopAndBottom(false);
        logWidget.setRenderBackground(false);
        this.addRenderableWidget(logWidget);
        ////

        info = new Button(leftPos + imageWidth / 2 + 80, topPos + imageHeight - 25, 50, 20, Component.literal("Info"), b -> moreInfo());
        addRenderableWidget(info);

        deliver = new Button(leftPos + imageWidth / 2 + 80, topPos + imageHeight - 25 - 25, 50, 20, Component.literal("Deliver"), b -> deliver());
        addRenderableWidget(deliver);


        initEditBox();

        int threeBYPos = topPos + 165;

        //this.setInitialFocus(this.name);
        home = new ImageButton(leftPos + imageWidth / 2 + 32, threeBYPos, 16, 16, 0, 0, 0, TownCampfires.id("textures/gui/home.png"), 16, 16,
                b -> ForgePacketHandler.sendToServer(new C2STownCampfireButtonPacket(C2STownCampfireButtonPacket.CampfireButton.SPAWN, townCampfire.location())));
        addRenderableWidget(home);

        bed = new BedButton(leftPos + imageWidth / 2 + 32 + 22, threeBYPos, 20, 20, Component.empty(),
                b -> ForgePacketHandler.sendToServer(new C2STownCampfireButtonPacket(C2STownCampfireButtonPacket.CampfireButton.BED, townCampfire.location())));
        addRenderableWidget(bed);

        gear = new ImageButton(leftPos + imageWidth / 2 + 32 + 22 * 2, threeBYPos, 20, 20, 0, 0, 0, TownCampfires.id("textures/gui/settings.png"), 20, 20, b -> {
        });
        addRenderableWidget(gear);

        startQuest = new Button(leftPos + imageWidth / 2 + 16, topPos + imageHeight - 25, 60, 20, TextComponents.START_QUEST,
                b -> pressStart(), this::startQuestTooltip);
        addRenderableWidget(startQuest);

        finishQuest = new Button(leftPos + imageWidth / 2 + 16, topPos + imageHeight - 25, 60, 20, Component.literal("Finish"), b -> pressFinish());
        addRenderableWidget(finishQuest);

        questTabSwitch = new ExtendedButton(leftPos + 70, topPos + 23, 66, 16, Component.literal("Available"),
                this::toggleQuestTab);
        addRenderableWidget(questTabSwitch);

        teleport = new Button(leftPos + imageWidth / 2 + 16, topPos + imageHeight - 25, 60, 20, Component.literal("Teleport"), b -> pressTeleport());
        addRenderableWidget(teleport);

        String s = "F";

        teleportFilter = new Button(leftPos + 10 + teleportSearch.getWidth(), topPos + TAB_HEIGHT + 4, 18, 16, Component.literal(s), b -> openTeleportFilter());
        addRenderableWidget(teleportFilter);

        logFilter = new Button(leftPos + 10 + logSearch.getWidth(), topPos + TAB_HEIGHT + 4, 18, 16,Component.literal(s), b -> openLogFilter());
        addRenderableWidget(logFilter);

        switchToTab(current);
    }

    void openTeleportFilter() {

    }

    void openLogFilter() {

    }

    void pressTeleport() {
        TeleportWidget.TeleportEntry selected = teleportWidget.getSelected();
        if (selected != null) {
            TownCampfire campfire = selected.campfire;
            if (!campfire.location().equals(townCampfire.location())) {
                ForgePacketHandler.sendToServer(new C2STeleportPacket(campfire.location()));
            } else {
                //message?
            }
        }
    }

    void startQuestTooltip(Button pButton, PoseStack pPoseStack, int pMouseX, int pMouseY) {
        QuestWidget.QuestEntry questEntry = questWidget.getSelected();
        if (questEntry != null) {
            Quest quest = questEntry.quest;

            List<Component> tooltip = new ArrayList<>();


            if (!TownCampfiresClient.hasEnoughSlots(quest)) {
                int freeSlots = TownCampfiresClient.getFreeSlots();
                int needed = quest.slots();
                tooltip.addAll(List.of(Component.literal("Insufficient slots: requires " + needed),
                        Component.literal("but only have " + freeSlots + " free slots")));
            }

            if (!TownCampfiresClient.canAttempt(questEntry.questID)) {
                tooltip.add(Component.literal("Out of attempts, wait for refresh"));
            }

            if (!tooltip.isEmpty()) {
                renderTooltip(pPoseStack, tooltip, Optional.empty(), pMouseX, pMouseY);
            }
        }
    }

    void pressStart() {
        QuestWidget.QuestEntry selected = questWidget.getSelected();
        if (selected != null) {
            ForgePacketHandler.sendToServer(new C2STownCampfireQuestPacket(TownCampfiresClient.clientLookup(selected.quest), townCampfire.location(),
                    C2STownCampfireQuestPacket.Type.START));
            startQuest.active = false;
        } else {
            ActiveQuestWidget.ActiveQuestEntry activeSelected = activeQuestWidget.getSelected();
            if (activeSelected != null) {
                ForgePacketHandler.sendToServer(new C2STownCampfireQuestPacket(activeSelected.questInstance.questID(), townCampfire.location(),
                        C2STownCampfireQuestPacket.Type.START));
                startQuest.active = false;
            }
        }
    }

    void pressFinish() {
        ActiveQuestWidget.ActiveQuestEntry selected = activeQuestWidget.getSelected();
        if (selected != null) {
            ForgePacketHandler.sendToServer(new C2STownCampfireQuestPacket(TownCampfiresClient.clientLookup(selected.questInstance.quest()),
                    townCampfire.location(), C2STownCampfireQuestPacket.Type.FINISH));
            finishQuest.active = false;
            deliver.active = false;
        }
    }

    void moreInfo() {
        QuestWidget.QuestEntry selected = questWidget.getSelected();
        Quest quest = null;
        if (selected != null) {
            quest = selected.quest;
        } else {
            ActiveQuestWidget.ActiveQuestEntry selectedA = activeQuestWidget.getSelected();
            if (selectedA != null) {
                quest = selectedA.questInstance.quest();
            }
        }
        if (quest != null) {
            Minecraft.getInstance().pushGuiLayer(new QuestInfoScreen(quest.name(), this, quest));
        }
    }

    void deliver() {
        ActiveQuestWidget.ActiveQuestEntry selected = activeQuestWidget.getSelected();
        if (selected != null) {
            ForgePacketHandler.sendToServer(new C2STownCampfireQuestPacket(TownCampfiresClient.clientLookup(selected.questInstance.quest()),
                    townCampfire.location(), C2STownCampfireQuestPacket.Type.DELIVER));
        }
    }

    void toggleQuestTab(Button b) {
        questTab = Utils.cycle(questTab);
        b.setMessage(Component.literal(questTab.name()));
        handleQuestTabs(current == Tab.quest);
    }

    void initEditBox() {
        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);

        int nameWidth = 90;

        int nameX = (imageWidth - nameWidth) / 2;

        this.name = new EditBox(this.font, leftPos + nameX + 28, topPos + TAB_HEIGHT + 8, nameWidth, 12,
                Component.translatable("towncampfires.container.name"));
        this.name.setTextColor(0xeeeeee);
        this.name.setTextColorUneditable(0xeeeeee);
        this.name.setBordered(false);
        this.name.setMaxLength(40);
        this.name.setResponder(this::onNameChanged);
        this.addWidget(this.name);

        this.teleportSearch = new EditBox(this.font, leftPos + 8, topPos + TAB_HEIGHT + 6, nameWidth, 12,
                Component.translatable("towncampfires.container.name"));
        this.teleportSearch.setTextColor(0xeeeeee);
        this.teleportSearch.setTextColorUneditable(0xeeeeee);
        //this.teleportSearch.setBordered(false);
        this.teleportSearch.setMaxLength(40);
        this.teleportSearch.setResponder(this::updateTeleportSearch);
        this.addWidget(this.teleportSearch);

        this.logSearch = new EditBox(this.font, leftPos + 8, topPos + TAB_HEIGHT + 6, nameWidth, 12,
                Component.translatable("towncampfires.container.name"));
        this.logSearch.setTextColor(0xeeeeee);
        this.logSearch.setTextColorUneditable(0xeeeeee);
        //this.teleportSearch.setBordered(false);
        this.logSearch.setMaxLength(40);
        this.logSearch.setResponder(this::updateLogSearch);
        this.addWidget(this.logSearch);

        teleportWidget.refresh();
        logWidget.refresh();
    }

    private void onNameChanged(String string) {
        if (!string.isBlank()) {
            ForgePacketHandler.sendToServer(new C2SSetTownCampfireNamePacket(string, townCampfire.location()));
        }
    }

    private void updateTeleportSearch(String s) {
        teleportWidget.update(s);
    }

    private void updateLogSearch(String s) {
        logWidget.update(s);
    }

    @Override
    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        String s = this.name.getValue();
        this.init(pMinecraft, pWidth, pHeight);
        this.name.setValue(s);
    }

    public void setToolTip(List<FormattedCharSequence> pToolTip) {
        this.toolTip = pToolTip;
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        int xSize = imageWidth;
        int ySize = imageHeight - TAB_HEIGHT + 4;
        RenderUtils.blitNineSlicedSized(pPoseStack, BACKGROUND, leftPos, topPos + TAB_HEIGHT,
                xSize, ySize, 4, 4, 12, 12, 0, 0, 12, 12);

    }

    public static final int DARK_GRAY = 0x404040;

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.toolTip = null;
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        this.name.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        this.teleportSearch.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        this.logSearch.render(pPoseStack, pMouseX, pMouseY, pPartialTick);


        if (townCampfire == null) return;

        int questCount = townCampfire.getAvailableQuests();

        switch (current) {
            case status -> {
                int y = 22;
                int h = 15;
                font.draw(pPoseStack, Component.literal("Villagers: " + townCampfire.getEffectiveVillagers() + "/" + townCampfire.getMaxVillagers()),
                        leftPos + 8, topPos + TAB_HEIGHT + y, DARK_GRAY);
                font.draw(pPoseStack, Component.literal("Blocks Allowed: " + townCampfire.getUsedBlocks() + "/" + townCampfire.getAllowedBlocks()),
                        leftPos + 8, topPos + TAB_HEIGHT + y + h, DARK_GRAY);

                font.draw(pPoseStack, Component.literal("Work Bench Limit: " + townCampfire.getUsedWorkbenches() + "/" + townCampfire.getAllowedWorkbenches()),
                        leftPos + 8, topPos + TAB_HEIGHT + y + h * 2, DARK_GRAY);

                font.draw(pPoseStack, Component.literal("Quests Available: " + questCount + "/" + townCampfire.getMaxQuests()),
                        leftPos + 8, topPos + TAB_HEIGHT + y + h * 3, DARK_GRAY);

                font.draw(pPoseStack, Component.literal("Refresh: " + TownCampfire.timeUntilRefresh(minecraft.level.getGameTime())),
                        leftPos + 8, topPos + TAB_HEIGHT + y + h * 4, DARK_GRAY);

                font.draw(pPoseStack, Component.literal("Raid Chance: " + "TODO"),
                        leftPos + 8, topPos + TAB_HEIGHT + y + h * 5, DARK_GRAY);
                renderExperience(pPoseStack, pMouseX, pMouseY, pPartialTick);

                font.draw(pPoseStack, Component.literal("Local Effects"),
                        leftPos + imageWidth / 2f + 32, topPos + TAB_HEIGHT + y, DARK_GRAY);
            }
            case quest -> {

                font.draw(pPoseStack, Component.literal("Quests: " + questCount + "/" + townCampfire.getMaxQuests()),
                        leftPos + 8, topPos + TAB_HEIGHT + 5, DARK_GRAY);


                int spacing = 12;

                switch (questTab) {
                    case current -> {
                        ActiveQuestWidget.ActiveQuestEntry questEntry = activeQuestWidget.getSelected();
                        if (questEntry != null) {
                            QuestInstance questInstance = questEntry.questInstance;
                            Quest quest = questInstance.quest();
                            int xStart = leftPos + imageWidth / 2;
                            int yStart = topPos + TAB_HEIGHT + 4;

                            itemRenderer.renderAndDecorateFakeItem(quest.icon(), xStart, yStart);

                            font.draw(pPoseStack, quest.compactName(), xStart + 20, yStart, 0xffffff);

                            int yLine = yStart + 18;

                            List<FormattedCharSequence> seq = quest.compactDesc().stream().map(Component::getVisualOrderText).toList();
                            for (FormattedCharSequence formattedCharSequence : seq) {
                                font.draw(pPoseStack, formattedCharSequence, xStart + 1, yLine, 0xffffff);
                                yLine += spacing;
                            }
                            font.draw(pPoseStack, Component.literal("Progress"), xStart, yLine, DARK_GRAY);
                            yLine += spacing;


                            List<QuestCriteria<?>> criterias = questInstance.quest().successCriteria().custom();
                            for (int i = 0; i < criterias.size(); i++) {
                                QuestCriteria<?> entry = criterias.get(i);
                                int count = entry.count();

                                int progress = questInstance.customProgress().isEmpty() ? 0 : questInstance.customProgress().get(i);

                                font.draw(pPoseStack, entry.desc().copy().append(" " + progress + "/" + count), xStart, yLine, 0xffffff);
                                yLine += spacing;
                            }


                            List<Delivery> deliveries = quest.successCriteria().deliveries();
                            for (int i = 0; i < deliveries.size(); i++) {
                                Delivery entry = deliveries.get(i);
                                int x0 = xStart + 40;
                                int x1 = x0 + 18;
                                int y0 = yLine;
                                int y1 = y0 + 18;
                                font.draw(pPoseStack, Component.literal("Deliver"), xStart, y0 + 3, DARK_GRAY);
                                ItemStack stack = entry.ingredient().getItems()[0].copy();
                                stack.setCount(entry.required());
                                minecraft.getItemRenderer().renderGuiItem(stack, x0, y0);
                                minecraft.getItemRenderer().renderGuiItemDecorations(font, stack, x0, y0, questInstance.deliveryProgress().get(i) + "/" + entry.required());
                                if (pMouseX > x0 && pMouseY > y0 && pMouseX < x1 && pMouseY < y1) {
                                    renderTooltip(pPoseStack, stack, pMouseX, pMouseY);
                                }
                                yLine += 18;
                            }


                            font.draw(pPoseStack, Component.literal("Members"), xStart, yLine, DARK_GRAY);

                            yLine += spacing;

                            StringBuilder allMembers = new StringBuilder();


                            for (UUID uuid : questInstance.getMembers()) {
                                String name = UsernameCache.getLastKnownUsername(uuid);
                                allMembers.append(name).append(",");
                            }
                            font.draw(pPoseStack, allMembers.toString(), xStart, yLine, DARK_GRAY);
                        }
                    }

                    case available -> {
                        QuestWidget.QuestEntry questEntry = questWidget.getSelected();
                        if (questEntry != null) {
                            Quest quest = questEntry.quest;
                            int xStart = leftPos + imageWidth / 2;
                            int yStart = topPos + TAB_HEIGHT + 6;
                            itemRenderer.renderAndDecorateFakeItem(quest.icon(), xStart, yStart);

                            font.draw(pPoseStack, quest.compactName(), xStart + 20, yStart, 0xffffff);
                            font.draw(pPoseStack, "Diff: " + quest.difficulty(), xStart + imageWidth / 2 - 40, yStart, DARK_GRAY);

                            List<FormattedCharSequence> seq = quest.compactDesc().stream().map(Component::getVisualOrderText).toList();
                            for (int i = 0; i < seq.size(); i++) {
                                FormattedCharSequence formattedCharSequence = seq.get(i);
                                font.draw(pPoseStack, formattedCharSequence, xStart + 1, yStart + 16 + 10 * i, 0xffffff);
                            }
                            int completeY = 88;
                            font.draw(pPoseStack, Component.literal("Complete Conditions"), xStart, yStart + completeY, DARK_GRAY);

                            Component mp_type = Component.literal(quest.type().name());
                            font.draw(pPoseStack, mp_type, xStart + imageWidth / 2 - 4 - font.width(mp_type), yStart + completeY, DARK_GRAY);

                            Component attempts = Component.literal("Attempts:" + quest.attempts());
                            font.draw(pPoseStack, attempts, xStart + imageWidth / 2 - 4 - font.width(attempts), yStart + completeY + 12, DARK_GRAY);

                            Component slots = Component.literal("Slots:" + quest.slots());
                            font.draw(pPoseStack, slots, xStart + imageWidth / 2 - 4 - font.width(slots), yStart + completeY + 12 * 2, DARK_GRAY);

                            Component xp = Component.literal("XP:" + quest.rewards().campfireExperience());
                            font.draw(pPoseStack, xp, xStart + imageWidth / 2 - 4 - font.width(xp), yStart + completeY + 12 * 3, DARK_GRAY);

                            List<QuestCriteria<?>> criterias = quest.successCriteria().custom();
                            for (int i = 0; i < criterias.size(); i++) {
                                QuestCriteria<?> entry = criterias.get(i);
                                int count = entry.count();
                                font.draw(pPoseStack, entry.desc().copy().append(" " + count), xStart, yStart + completeY + 10 + 10 * i, 0xffffff);
                            }

                            List<Delivery> deliveries = quest.successCriteria().deliveries();
                            for (int i = 0; i < deliveries.size(); i++) {
                                Delivery entry = deliveries.get(i);
                                int x0 = xStart + 40;
                                int x1 = x0 + 18;
                                int y0 = yStart + completeY + 12 + 18 * i;
                                int y1 = y0 + 18;
                                font.draw(pPoseStack, Component.literal("Deliver"), xStart, y0, DARK_GRAY);
                                ItemStack stack = entry.ingredient().getItems()[0].copy();
                                stack.setCount(entry.required());
                                minecraft.getItemRenderer().renderGuiItem(stack, x0, y0);
                                minecraft.getItemRenderer().renderGuiItemDecorations(font, stack, x0, y0);
                                if (pMouseX > x0 && pMouseY > y0 && pMouseX < x1 && pMouseY < y1) {
                                    renderTooltip(pPoseStack, stack, pMouseX, pMouseY);
                                }
                            }
                        }
                    }
                }
            }
            case teleport -> {
                TeleportWidget.TeleportEntry selected = teleportWidget.getSelected();
                if (selected != null) {
                    int yLine = topPos + TAB_HEIGHT + 108;
                    int startX = leftPos + imageWidth / 2 + 8;
                    TownCampfire lookingAt = selected.campfire;
                    font.draw(pPoseStack, lookingAt.name(), startX, yLine, DARK_GRAY);
                    font.draw(pPoseStack, "Level: " + lookingAt.getLevel(), startX, yLine + 12, DARK_GRAY);
                    font.draw(pPoseStack, "Quests: " + lookingAt.getMaxQuests(), startX, yLine + 24, DARK_GRAY);
                    font.draw(pPoseStack, "Biome: " + Minecraft.getInstance().level.getBiome(townCampfire.location()).unwrapKey().get().location(), startX, yLine + 36, DARK_GRAY);
                    font.draw(pPoseStack, "Villagers: " + lookingAt.getEffectiveVillagers(), startX, yLine + 48, DARK_GRAY);
                    font.draw(pPoseStack, "Distance: " + DECIMAL_FORMAT.format(Math.sqrt(lookingAt.location().distSqr(townCampfire.location()))), startX, yLine + 60, DARK_GRAY);

                    DynamicTexture texture = selected.screenshot;
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    ResourceLocation iconLocation = selected.screenshotLocation;
                    RenderSystem.setShaderTexture(0, texture != null ? iconLocation : ICON_MISSING);
                    RenderSystem.enableBlend();
                    int screenW = 144;
                    int screenH = 96;
                    GuiComponent.blit(pPoseStack, startX, topPos + TAB_HEIGHT + 6, 0.0F, 0.0F, screenW, screenH, screenW, screenH);
                    RenderSystem.disableBlend();
                }
            } case logs -> {

                LogWidget.LogEntry questEntry = logWidget.getSelected();
                if (questEntry != null && questEntry.quest != null) {
                    Quest quest = questEntry.quest;
                    int xStart = leftPos + imageWidth / 2;
                    int yStart = topPos + TAB_HEIGHT + 6;
                    itemRenderer.renderAndDecorateFakeItem(quest.icon(), xStart, yStart);

                    font.draw(pPoseStack, quest.compactName(), xStart + 20, yStart, 0xffffff);
                    font.draw(pPoseStack, "Diff: " + quest.difficulty(), xStart + imageWidth / 2 - 40, yStart, DARK_GRAY);

                    List<FormattedCharSequence> seq = quest.compactDesc().stream().map(Component::getVisualOrderText).toList();
                    for (int i = 0; i < seq.size(); i++) {
                        FormattedCharSequence formattedCharSequence = seq.get(i);
                        font.draw(pPoseStack, formattedCharSequence, xStart + 1, yStart + 16 + 10 * i, 0xffffff);
                    }
                    int completeY = 88;
                    font.draw(pPoseStack, Component.literal("Complete Conditions"), xStart, yStart + completeY, DARK_GRAY);

                    Component mp_type = Component.literal(quest.type().name());
                    font.draw(pPoseStack, mp_type, xStart + imageWidth / 2 - 4 - font.width(mp_type), yStart + completeY, DARK_GRAY);

                    Component attempts = Component.literal("Attempts:" + quest.attempts());
                    font.draw(pPoseStack, attempts, xStart + imageWidth / 2 - 4 - font.width(attempts), yStart + completeY + 12, DARK_GRAY);

                    Component slots = Component.literal("Slots:" + quest.slots());
                    font.draw(pPoseStack, slots, xStart + imageWidth / 2 - 4 - font.width(slots), yStart + completeY + 12 * 2, DARK_GRAY);

                    Component xp = Component.literal("XP:" + quest.rewards().campfireExperience());
                    font.draw(pPoseStack, xp, xStart + imageWidth / 2 - 4 - font.width(xp), yStart + completeY + 12 * 3, DARK_GRAY);

                    List<QuestCriteria<?>> criterias = quest.successCriteria().custom();
                    for (int i = 0; i < criterias.size(); i++) {
                        QuestCriteria<?> entry = criterias.get(i);
                        int count = entry.count();
                        font.draw(pPoseStack, entry.desc().copy().append(" " + count), xStart, yStart + completeY + 10 + 10 * i, 0xffffff);
                    }

                    List<Delivery> deliveries = quest.successCriteria().deliveries();
                    for (int i = 0; i < deliveries.size(); i++) {
                        Delivery entry = deliveries.get(i);
                        int x0 = xStart + 40;
                        int x1 = x0 + 18;
                        int y0 = yStart + completeY + 12 + 18 * i;
                        int y1 = y0 + 18;
                        font.draw(pPoseStack, Component.literal("Deliver"), xStart, y0, DARK_GRAY);
                        ItemStack stack = entry.ingredient().getItems()[0].copy();
                        stack.setCount(entry.required());
                        minecraft.getItemRenderer().renderGuiItem(stack, x0, y0);
                        minecraft.getItemRenderer().renderGuiItemDecorations(font, stack, x0, y0);
                        if (pMouseX > x0 && pMouseY > y0 && pMouseX < x1 && pMouseY < y1) {
                            renderTooltip(pPoseStack, stack, pMouseX, pMouseY);
                        }
                    }
                }

            }
        }


        if (this.toolTip != null) {
            this.renderTooltip(pPoseStack, this.toolTip, pMouseX, pMouseY);
        }
    }

    static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    static final ResourceLocation ICON_MISSING = new ResourceLocation("textures/misc/unknown_server.png");

    void renderExperience(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
        int xPos = leftPos + imageWidth / 2 - 91;
        int yPos = topPos + imageHeight - 22;
        int j = 182;
        long experience = townCampfire.getExperience();
        int experiencePerLevel = TownCampfireConfig.CONFIG.experience_per_level.get();

        float scale = (((float) experience % experiencePerLevel) / experiencePerLevel) * 183f;

        this.blit(pPoseStack, xPos, yPos, 0, 64, j, 5);
        if (scale > 0) {
            this.blit(pPoseStack, xPos, yPos, 0, 69, (int) scale, 5);
        }

        String level = "Level: " + townCampfire.getLevel();

        font.draw(pPoseStack, Component.literal(level),
                leftPos + imageWidth / 2f - font.width(level) / 2f, yPos - 15, DARK_GRAY);

        String progress = (experience % experiencePerLevel) + "/" + experiencePerLevel;

        font.draw(pPoseStack, Component.literal(progress),
                leftPos + imageWidth / 2f - font.width(progress) / 2f, yPos + 10, DARK_GRAY);

    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.player.closeContainer();
        }

        return this.name.keyPressed(pKeyCode, pScanCode, pModifiers) || this.name.canConsumeInput() ||
                this.teleportSearch.keyPressed(pKeyCode, pScanCode, pModifiers) || this.teleportSearch.canConsumeInput() || super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (townCampfire != null) {
            double distance = minecraft.player.blockPosition().distSqr(townCampfire.location());
            if (distance > 64) {
                Minecraft.getInstance().setScreen(null);
            }
        }
    }

    QuestTab questTab = QuestTab.available;

    enum QuestTab {
        available, current;
    }

    protected void switchToTab(Tab tab) {
        current = tab;

        boolean mainTab = tab == Tab.status;
        name.setEditable(mainTab);
        name.setVisible(mainTab);
////////
        boolean teleportTab = tab == Tab.teleport;
        teleportSearch.setEditable(teleportTab);
        teleportSearch.setVisible(teleportTab);
        teleportWidget.setVisible(teleportTab);
        teleport.visible = teleportTab;
        teleportFilter.visible = teleportTab;
////////
        boolean logTab = tab == Tab.logs;
        logWidget.setVisible(logTab);
        logSearch.setEditable(logTab);
        logSearch.setVisible(logTab);
        logFilter.visible = logTab;


        home.visible = bed.visible = gear.visible = mainTab;
        campfireEffectWidget.setVisible(mainTab);

        questTabSwitch.visible = tab == Tab.quest;

        handleQuestTabs(current == Tab.quest);
        if (tab == Tab.trades) {
            ForgePacketHandler.sendToServer(new C2STownCampfireButtonPacket(C2STownCampfireButtonPacket.CampfireButton.TRADE, townCampfire.location()));
        }
    }


    protected void handleQuestTabs(boolean visible) {
        questWidget.setVisible(visible && questTab == QuestTab.available);
        activeQuestWidget.setVisible(visible && questTab == QuestTab.current);
        questWidget.setSelected(null);
        activeQuestWidget.setSelected(null);

        updateStartQuest(visible);

        ActiveQuestWidget.ActiveQuestEntry e = activeQuestWidget.getSelected();

        finishQuest.visible = visible && questTab == QuestTab.current && e != null;
        info.visible = visible && (questTab == QuestTab.current && (questWidget.getSelected() != null || e != null));
        deliver.visible = visible && questTab == QuestTab.available && e != null && !e.questInstance.quest().successCriteria().deliveries().isEmpty();
    }

    protected void updateStartQuest(boolean visible) {
        QuestWidget.QuestEntry selected = questWidget.getSelected();
        startQuest.visible = visible && questTab == QuestTab.available && selected != null && isQuestAvailable(selected.quest);
        startQuest.active = startQuest.visible && TownCampfiresClient.hasEnoughSlots(selected.quest) && TownCampfiresClient.canAttempt(selected.questID);
        if (selected != null && selected.quest != null) {
            boolean needsPrep = selected.quest.type() != Quest.MultiplayerType.solo;
            startQuest.setMessage(needsPrep ? TextComponents.PREP_QUEST : TextComponents.START_QUEST);
        }
    }


    public boolean isQuestAvailable(Quest quest) {
        ResourceLocation questID = TownCampfiresClient.clientLookup(quest);
        for (QuestInstance currentQuest : TownCampfiresClient.currentQuests) {
            ResourceLocation id = currentQuest.questID();
            if (Objects.equals(questID, id) && currentQuest.status().active) {
                return false;
            }
        }
        return true;
    }

    public class TabButton extends Button {
        private final Tab tab;

        public TabButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress, Tab tab) {
            super(pX, pY, pWidth, pHeight, pMessage, pOnPress);
            this.tab = tab;
        }

        public TabButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress, OnTooltip pOnTooltip, Tab tab) {
            super(pX, pY, pWidth, pHeight, pMessage, pOnPress, pOnTooltip);
            this.tab = tab;
        }

        @Override
        public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            boolean isSelected = tab == current;
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, BACKGROUND);
            RenderSystem.enableDepthTest();
            blitNineSlicedSizedTab(pPoseStack, BACKGROUND, x, y,
                    this.width, this.height + (isSelected ? 4 : 0), 4, 4, 12, 12, 0, 0, 12, 12);
            int j = getFGColor();
            drawCenteredString(pPoseStack, font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, j | Mth.ceil(this.alpha * 255.0F) << 24);

            if (this.isHovered) {
                this.renderToolTip(pPoseStack, pMouseX, pMouseY);
            }
        }

        public static void drawCenteredString(PoseStack pPoseStack, Font pFont, Component pText, int pX, int pY, int pColor) {
            FormattedCharSequence formattedcharsequence = pText.getVisualOrderText();
            pFont.draw(pPoseStack, formattedcharsequence, (float) (pX - pFont.width(formattedcharsequence) / 2), (float) pY, pColor);
        }


        /**
         * backport of 1.20 nineSlice
         *
         * @param texture
         * @param x
         * @param y
         * @param width
         * @param height
         * @param sliceSize
         * @param uWidth
         * @param vHeight
         * @param uOffset
         * @param vOffset
         * @param textureWidth
         * @param textureHeight
         */
        public static void blitNineSlicedSizedTab(PoseStack stack, ResourceLocation texture, int x, int y, int width, int height,
                                                  int sliceSize, int uWidth, int vHeight, int uOffset, int vOffset, int textureWidth, int textureHeight) {
            blitNineSlicedSizedTab(stack, texture, x, y, width, height, sliceSize, sliceSize, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
        }

        public static void blitNineSlicedSizedTab(PoseStack stack, ResourceLocation texture, int x, int y, int width, int height, int sliceWidth,
                                                  int sliceHeight, int uWidth, int vHeight, int uOffset, int vOffset, int textureWidth, int textureHeight) {
            blitNineSlicedSizedTab(stack, texture, x, y, width, height, sliceWidth, sliceHeight, sliceWidth, sliceHeight, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
        }

        public static void blitNineSlicedSizedTab(PoseStack stack, ResourceLocation texture, int x, int y, int width, int height, int cornerWidth, int cornerHeight,
                                                  int edgeWidth, int edgeHeight, int uWidth, int vHeight, int uOffset, int vOffset, int textureWidth, int textureHeight) {
            cornerWidth = Math.min(cornerWidth, width / 2);
            edgeWidth = Math.min(edgeWidth, width / 2);
            cornerHeight = Math.min(cornerHeight, height / 2);
            edgeHeight = Math.min(edgeHeight, height / 2);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, texture);
            if (width == uWidth && height == vHeight) {
                GuiComponent.blit(stack, x, y, (float) uOffset, (float) vOffset, width, height, textureWidth, textureHeight);
            } else if (height == vHeight) {
                GuiComponent.blit(stack, x, y, (float) uOffset, (float) vOffset, cornerWidth, height, textureWidth, textureHeight);
                RenderUtils.blitRepeating(stack, x + cornerWidth, y, width - edgeWidth - cornerWidth, height,
                        uOffset + cornerWidth, vOffset, uWidth - edgeWidth - cornerWidth, vHeight, textureWidth, textureHeight);
                GuiComponent.blit(stack, x + width - edgeWidth, y, (float) (uOffset + uWidth - edgeWidth), (float) vOffset, edgeWidth, height, textureWidth, textureHeight);
            } else if (width == uWidth) {
                GuiComponent.blit(stack, x, y, (float) uOffset, (float) vOffset, width, cornerHeight, textureWidth, textureHeight);
                RenderUtils.blitRepeating(stack, x, y + cornerHeight, width, height - edgeHeight - cornerHeight, uOffset,
                        vOffset + cornerHeight, uWidth, vHeight - edgeHeight - cornerHeight, textureWidth, textureHeight);
                GuiComponent.blit(stack, x, y + height - edgeHeight, (float) uOffset, (float) (vOffset + vHeight - edgeHeight), width, edgeHeight, textureWidth, textureHeight);
            } else {
                GuiComponent.blit(stack, x, y, (float) uOffset, (float) vOffset, cornerWidth, cornerHeight, textureWidth, textureHeight);
                RenderUtils.blitRepeating(stack, x + cornerWidth, y, width - edgeWidth - cornerWidth, cornerHeight,
                        uOffset + cornerWidth, vOffset, uWidth - edgeWidth - cornerWidth, cornerHeight, textureWidth, textureHeight);
                GuiComponent.blit(stack, x + width - edgeWidth, y, (float) (uOffset + uWidth - edgeWidth), (float) vOffset, edgeWidth, cornerHeight, textureWidth, textureHeight);

                //bottom left
                //GuiComponent.blit(stack, x, y + height - edgeHeight, (float) uOffset, (float) (vOffset + vHeight - edgeHeight), cornerWidth, edgeHeight, textureWidth, textureHeight);

                //bottom
                //blitRepeating(stack, x + cornerWidth, y + height - edgeHeight, width - edgeWidth - cornerWidth, edgeHeight, uOffset + cornerWidth,
                // vOffset + vHeight - edgeHeight, uWidth - edgeWidth - cornerWidth, edgeHeight, textureWidth, textureHeight);

                //bottom right
                //GuiComponent.blit(stack, x + width - edgeWidth, y + height - edgeHeight, (float) (uOffset + uWidth - edgeWidth), (float) (vOffset + vHeight - edgeHeight), edgeWidth, edgeHeight, textureWidth, textureHeight);
                //left
                RenderUtils.blitRepeating(stack, x, y + cornerHeight, cornerWidth, height - edgeHeight - cornerHeight, uOffset,
                        vOffset + cornerHeight, cornerWidth, vHeight - edgeHeight - cornerHeight, textureWidth, textureHeight);
                //middle
                RenderUtils.blitRepeating(stack, x + cornerWidth, y + cornerHeight, width - edgeWidth - cornerWidth, height - edgeHeight - cornerHeight,
                        uOffset + cornerWidth, vOffset + cornerHeight, uWidth - edgeWidth - cornerWidth, vHeight - edgeHeight - cornerHeight, textureWidth, textureHeight);
                //right
                RenderUtils.blitRepeating(stack, x + width - edgeWidth, y + cornerHeight, cornerWidth, height - edgeHeight - cornerHeight,
                        uOffset + uWidth - edgeWidth, vOffset + cornerHeight, edgeWidth, vHeight - edgeHeight - cornerHeight, textureWidth, textureHeight);
            }
        }
    }

    public <T extends ObjectSelectionList.Entry<T>> void buildList(List<ResourceLocation> list,
                                                                   Consumer<T> consumer, Function<ResourceLocation, T> newEntry) {
        list.forEach(location -> {
            T entry = newEntry.apply(location);
            if (entry != null) {
                consumer.accept(entry);
            }
        });
    }

    protected class CampfireEffectWidget extends ObjectSelectionList<CampfireEffectWidget.CampfireEffectEntry> {

        boolean visible;

        public CampfireEffectWidget(Minecraft pMinecraft, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight) {
            super(pMinecraft, pWidth, pHeight, pY0, pY1, pItemHeight);
            if (TownCampfireScreen.this.townCampfire != null) {
                refreshList();
            }
        }

        @Override
        public int getRowWidth() {
            return width;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public void refreshList() {
            this.clearEntries();
            buildList(townCampfire.getEffectIds(), this::addEntry, location -> new CampfireEffectEntry(TownCampfiresClient.campfireEffectLoader.getCampfireEffects().get(location)));
        }

        @Override
        public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            if (visible) {
                //left,bottom,right,top
                GuiComponent.enableScissor(x0, y0, x1 + 10, y1);
                super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
                GuiComponent.disableScissor();
            }
        }

        @Override
        protected int getScrollbarPosition() {
            return x1;
        }

        public class CampfireEffectEntry extends ObjectSelectionList.Entry<CampfireEffectEntry> {
            private final CampfireEffect effect;

            CampfireEffectEntry(CampfireEffect info) {
                this.effect = info;
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", effect.name());
            }

            @Override
            public void render(PoseStack poseStack, int entryIdx, int top, int left, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
                Font font = TownCampfireScreen.this.font;
                font.draw(poseStack, effect.name(), left, top, DARK_GRAY);

                if (isMouseOver) {
                    List<FormattedCharSequence> seq = effect.desc().stream().map(Component::getVisualOrderText).toList();
                    TownCampfireScreen.this.setToolTip(seq);
                }

                //    font.draw(poseStack, Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(name,
                //    listWidth))), left + 3, top + 2, 0xFFFFFF);
                //    font.draw(poseStack, Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(version, listWidth))),
                //    left + 3, top + 2 + font.lineHeight, 0xCCCCCC);

            }

            public CampfireEffect getEffect() {
                return effect;
            }
        }
    }

    protected class TeleportWidget extends ObjectSelectionList<TeleportWidget.TeleportEntry> {

        boolean visible;

        public TeleportWidget(Minecraft pMinecraft, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight) {
            super(pMinecraft, pWidth, pHeight, pY0, pY1, pItemHeight);
        }

        @Override
        public int getRowWidth() {
            return width;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public void refresh() {
            update(teleportSearch.getValue());
        }

        void update(String s) {
            this.clearEntries();
            setScrollAmount(0);
            teleports.forEach(townCampfire -> {
                String name = townCampfire.name().getString();
                if (name.contains(s)) {
                    addEntry(new TeleportEntry(townCampfire));
                }
            });
        }

        @Override
        public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            if (visible) {
                //left,bottom,right,top
                GuiComponent.enableScissor(x0, y0, x1 + 10, y1);
                super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
                GuiComponent.disableScissor();
            }
        }

        @Override
        public boolean isMouseOver(double pMouseX, double pMouseY) {
            return visible && super.isMouseOver(pMouseX, pMouseY);
        }

        @Override
        public void setSelected(@Nullable TownCampfireScreen.TeleportWidget.TeleportEntry pSelected) {
            super.setSelected(pSelected);
            teleport.active = pSelected != null && Math.sqrt(pSelected.campfire.location().distSqr(townCampfire.location())) <= TownCampfireConfig.CONFIG.unteleportable_distance.get();
        }

        @Override
        protected int getScrollbarPosition() {
            return x1;
        }

        public class TeleportEntry extends ObjectSelectionList.Entry<TeleportEntry> implements AutoCloseable {
            private final TownCampfire campfire;
            ResourceLocation screenshotLocation;
            Path screenshotFile;
            @Nullable
            DynamicTexture screenshot;

            TeleportEntry(TownCampfire info) {
                this.campfire = info;
                String s = TownCampfiresClient.getScreenshotName(minecraft,info.location());
                screenshotFile = minecraft.gameDirectory.toPath().resolve("screenshots").resolve(s);
                screenshotLocation = new ResourceLocation("minecraft", "screenshots/" + Util.sanitizeName(s,
                        ResourceLocation::validPathChar));

                if (!Files.isRegularFile(this.screenshotFile)) {
                    this.screenshotFile = null;
                }

                this.screenshot = loadScreenshot();
            }

            @Nullable
            private DynamicTexture loadScreenshot() {
                boolean flag = this.screenshotFile != null && Files.isRegularFile(this.screenshotFile);
                if (flag) {
                    try {
                        InputStream inputstream = Files.newInputStream(screenshotFile);

                        DynamicTexture dynamicTexture;
                        try {
                            NativeImage nativeimage = NativeImage.read(inputstream);
                            //   Validate.validState(nativeimage.getWidth() == 64, "Must be 64 pixels wide");
                            //   Validate.validState(nativeimage.getHeight() == 64, "Must be 64 pixels high");
                            DynamicTexture dynamictexture = new DynamicTexture(nativeimage);
                            minecraft.getTextureManager().register(screenshotLocation, dynamictexture);
                            dynamicTexture = dynamictexture;
                        } catch (Throwable throwable1) {
                            try {
                                inputstream.close();
                            } catch (Throwable throwable) {
                                throwable1.addSuppressed(throwable);
                            }

                            throw throwable1;
                        }

                        inputstream.close();
                        return dynamicTexture;
                    } catch (Throwable throwable2) {
                        throwable2.printStackTrace();
                        return null;
                    }
                } else {
                    minecraft.getTextureManager().release(screenshotLocation);
                    return null;
                }
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", campfire.name());
            }

            @Override
            public void render(PoseStack poseStack, int entryIdx, int top, int left, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
                Font font = TownCampfireScreen.this.font;
                font.draw(poseStack, campfire.name(), left, top, DARK_GRAY);

                if (isMouseOver) {
                    // List<FormattedCharSequence> seq = campfire.desc().stream().map(Component::getVisualOrderText).toList();
                    //TownCampfireScreen.this.setToolTip(seq);
                }

                //    font.draw(poseStack, Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(name,
                //    listWidth))), left + 3, top + 2, 0xFFFFFF);
                //    font.draw(poseStack, Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(version, listWidth))),
                //    left + 3, top + 2 + font.lineHeight, 0xCCCCCC);

            }

            @Override
            public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
                setSelected(this);
                return true;
            }

            @Override
            public void close() {
                if (this.screenshot != null) {
                    this.screenshot.close();
                }
            }
        }
    }

    protected class LogWidget extends ObjectSelectionList<LogWidget.LogEntry> {

        boolean visible;

        public LogWidget(Minecraft pMinecraft, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight) {
            super(pMinecraft, pWidth, pHeight, pY0, pY1, pItemHeight);
        }

        @Override
        public int getRowWidth() {
            return width;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public void refresh() {
            update(logSearch.getValue());
        }

        void update(String s) {
            this.clearEntries();
            setScrollAmount(0);
            TownCampfiresClient.logs.forEach(component -> {
                String name = component.log().getString();
                if (name.contains(s)) {
                    addEntry(new LogEntry(component));
                }
            });
        }

        @Override
        public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            if (visible) {
                //left,bottom,right,top
                GuiComponent.enableScissor(x0, y0, x1 + 10, y1);
                super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
                GuiComponent.disableScissor();
            }
        }

        @Override
        public void setSelected(@Nullable LogEntry pSelected) {
            super.setSelected(pSelected);
        }

        @Override
        public boolean isMouseOver(double pMouseX, double pMouseY) {
            return visible && super.isMouseOver(pMouseX, pMouseY);
        }

        @Override
        protected int getScrollbarPosition() {
            return x1;
        }

        public class LogEntry extends ObjectSelectionList.Entry<LogEntry> {
            private final List<FormattedCharSequence> split;
            private final @Nullable Quest quest;
            LogEntry(QuestLogEntry questLogEntry) {
                this.quest = TownCampfiresClient.questLoader.getQuestMap().get(questLogEntry.questID());
                split = font.split(questLogEntry.log(), width);
            }

            @Override
            public Component getNarration() {
                return Component.empty();//Component.translatable("narrator.select", log);
            }

            @Override
            public void render(PoseStack poseStack, int entryIdx, int top, int left, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
                Font font = TownCampfireScreen.this.font;
                for (int i = 0; i < split.size();i++) {
                    FormattedCharSequence c = split.get(i);
                    font.draw(poseStack, c, left, top+i * font.lineHeight, DARK_GRAY);
                }
            }

            @Override
            public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
                setSelected(this);
                return true;
            }
        }
    }

    @Override
    public void removed() {
        if (this.teleportWidget != null) {
            this.teleportWidget.children().forEach(TeleportWidget.TeleportEntry::close);
        }
    }



    protected class QuestWidget extends ObjectSelectionList<QuestWidget.QuestEntry> {

        boolean visible;

        public QuestWidget(Minecraft pMinecraft, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight) {
            super(pMinecraft, pWidth, pHeight, pY0, pY1, pItemHeight);
            if (TownCampfireScreen.this.townCampfire != null) {
                refreshList();
            }
        }

        @Override
        public void setSelected(@org.jetbrains.annotations.Nullable TownCampfireScreen.QuestWidget.QuestEntry pSelected) {
            super.setSelected(pSelected);
            if (pSelected != null && isQuestAvailable(pSelected.quest)) {
                TownCampfireScreen.this.updateStartQuest(true);
                info.visible = true;
            }
        }

        @Override
        public boolean isMouseOver(double pMouseX, double pMouseY) {
            return visible && super.isMouseOver(pMouseX, pMouseY);
        }

        @Override
        public int getRowWidth() {
            return width;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public void refreshList() {
            this.clearEntries();
            buildList(townCampfire.getQuestIds(), this::addEntry, location -> {
                if (TownCampfiresClient.isQuestAlreadyActive(location)) {
                    return null;
                }
                return new QuestEntry(location, TownCampfiresClient.questLoader.getQuestMap().get(location));
            });
        }

        @Override
        public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            if (visible) {
                //left,bottom,right,top
                GuiComponent.enableScissor(x0, y0, x1 + 10, y1);
                super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
                GuiComponent.disableScissor();
            }
        }

        @Override
        protected int getScrollbarPosition() {
            return x1;
        }

        public class QuestEntry extends ObjectSelectionList.Entry<QuestEntry> {
            private final ResourceLocation questID;
            private final Quest quest;

            QuestEntry(ResourceLocation questID, Quest info) {
                this.questID = questID;
                this.quest = info;
            }

            @Override
            public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
                setSelected(this);
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", quest.name());
            }

            @Override
            public void render(PoseStack poseStack, int entryIdx, int top, int left, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
                Font font = TownCampfireScreen.this.font;
                itemRenderer.renderAndDecorateFakeItem(quest.icon(), left, top);
                font.draw(poseStack, quest.compactName(), left + 20, top, 0xffffff);
                font.draw(poseStack, "Diff: " + quest.difficulty(), left + entryWidth - 32, top, 0xffffff);

                List<FormattedCharSequence> seq = quest.compactDesc().stream().map(Component::getVisualOrderText).toList();
                for (int i = 0; i < seq.size(); i++) {
                    FormattedCharSequence formattedCharSequence = seq.get(i);
                    font.draw(poseStack, formattedCharSequence, left + 1, top + 18 + 10 * i, 0xffffff);
                }

                //TownCampfireScreen.this.setToolTip(seq);

                //    font.draw(poseStack, Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(name,
                //    listWidth))), left + 3, top + 2, 0xFFFFFF);
                //    font.draw(poseStack, Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(version, listWidth))),
                //    left + 3, top + 2 + font.lineHeight, 0xCCCCCC);

            }

            public Quest getQuest() {
                return quest;
            }
        }
    }

    protected class ActiveQuestWidget extends ObjectSelectionList<ActiveQuestWidget.ActiveQuestEntry> {

        boolean visible;

        public ActiveQuestWidget(Minecraft pMinecraft, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight) {
            super(pMinecraft, pWidth, pHeight, pY0, pY1, pItemHeight);
            refreshList();
        }

        @Override
        public void setSelected(@org.jetbrains.annotations.Nullable ActiveQuestEntry pSelected) {
            super.setSelected(pSelected);
            if (pSelected != null) {
                QuestInstance questInstance = pSelected.questInstance;
                QuestInstance.Status status = questInstance.status();
                switch (status) {
                    case PREP -> {
                        finishQuest.visible = false;
                        startQuest.visible = true;
                        startQuest.active = true;
                        startQuest.setMessage(TextComponents.START_QUEST);
                    }
                    case IN_PROGRESS -> {
                        finishQuest.visible = true;
                        info.visible = true;
                        finishQuest.active = false;
                        deliver.visible = true;
                    }
                    case COMPLETE -> {
                        finishQuest.visible = true;
                        info.visible = true;
                        finishQuest.active = true;
                        deliver.visible = false;
                    }
                }
            }
        }

        @Override
        public void replaceEntries(Collection<ActiveQuestEntry> pEntries) {
            super.replaceEntries(pEntries);
        }

        @Override
        public int getRowWidth() {
            return width;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        QuestInstance temp;

        public void refreshList() {
            temp = getSelected() != null ? getSelected().questInstance : null;
            this.clearEntries();
            for (QuestInstance questInstance : TownCampfiresClient.currentQuests) {
                ActiveQuestEntry questEntry = new ActiveQuestEntry(questInstance);
                addEntry(questEntry);
                if (temp != null && questInstance.questID().equals(temp.questID())) {
                    setSelected(questEntry);
                }
            }
        }

        @Override
        public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            if (visible) {
                //left,bottom,right,top
                GuiComponent.enableScissor(x0, y0, x1 + 10, y1);
                super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
                GuiComponent.disableScissor();
            }
        }

        @Override
        public boolean isMouseOver(double pMouseX, double pMouseY) {
            return visible && super.isMouseOver(pMouseX, pMouseY);
        }

        @Override
        protected int getScrollbarPosition() {
            return x1;
        }

        public class ActiveQuestEntry extends ObjectSelectionList.Entry<ActiveQuestEntry> {
            private final QuestInstance questInstance;

            ActiveQuestEntry(QuestInstance info) {
                this.questInstance = info;
            }

            @Override
            public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
                setSelected(this);
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", questInstance.quest().name());
            }

            @Override
            public void render(PoseStack poseStack, int entryIdx, int top, int left, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
                Font font = TownCampfireScreen.this.font;
                itemRenderer.renderAndDecorateFakeItem(questInstance.quest().icon(), left, top);
                font.draw(poseStack, questInstance.quest().name(), left + 20, top, 0xffffff);

                List<FormattedCharSequence> seq = questInstance.quest().desc().stream().map(Component::getVisualOrderText).toList();
                for (int i = 0; i < seq.size(); i++) {
                    FormattedCharSequence formattedCharSequence = seq.get(i);
                    font.draw(poseStack, formattedCharSequence, left + 1, top + 18 + 10 * i, 0xffffff);
                }

                //TownCampfireScreen.this.setToolTip(seq);

                //    font.draw(poseStack, Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(name,
                //    listWidth))), left + 3, top + 2, 0xFFFFFF);
                //    font.draw(poseStack, Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(version, listWidth))),
                //    left + 3, top + 2 + font.lineHeight, 0xCCCCCC);

            }

            public QuestInstance getQuestInstance() {
                return questInstance;
            }
        }
    }
}
