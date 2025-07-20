package tfar.towncampfires.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import net.minecraftforge.common.UsernameCache;
import org.lwjgl.glfw.GLFW;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.config.TownCampfireConfig;
import tfar.towncampfires.data.CampfireEffect;
import tfar.towncampfires.data.quest.Quest;
import tfar.towncampfires.data.quest.QuestCriteria;
import tfar.towncampfires.data.quest.QuestInstance;
import tfar.towncampfires.network.ForgePacketHandler;
import tfar.towncampfires.network.server.C2SSetTownCampfireNamePacket;
import tfar.towncampfires.network.server.C2STownCampfireButtonPacket;
import tfar.towncampfires.network.server.C2STownCampfireQuestPacket;
import tfar.towncampfires.utils.Utils;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class TownCampfireScreen extends BasicScreen {

    protected EditBox name;
    @Nullable
    private List<FormattedCharSequence> toolTip;

    private TownCampfire townCampfire;

    public void setCampfire(TownCampfire townCampfire) {
        this.townCampfire = townCampfire;
        name.setValue(townCampfire.name().getString());
        campfireEffectWidget.refreshList();
        questWidget.refreshList();

    }

    public void updateQuests() {
        activeQuestWidget.refreshList();
    }

    public enum Tab {
        status,quest,trades,teleport,logs
    }

    protected static final int TAB_HEIGHT = 20;

    protected Tab current = Tab.status;
    protected Button home;
    protected Button bed;
    protected Button gear;

    protected CampfireEffectWidget campfireEffectWidget;
    protected QuestWidget questWidget;
    protected ActiveQuestWidget activeQuestWidget;

    protected Button startQuest;

    protected Button finishQuest;

    protected Button questTabSwitch;
    protected Button info;

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
            TabButton tabButton = new TabButton(xPos,topPos,tabWidth,TAB_HEIGHT+4,Component.literal(tab.name()),pButton -> switchToTab(tab),tab);
            tabButton.setFGColor(DARK_GRAY);
            addRenderableWidget(tabButton);
        }

        int yPos = topPos+55;

        int statusHeight = 100;

        campfireEffectWidget = new CampfireEffectWidget(minecraft,160,statusHeight,yPos,yPos+statusHeight,20);
        campfireEffectWidget.setLeftPos(leftPos+150);
        campfireEffectWidget.setRenderTopAndBottom(false);
        campfireEffectWidget.setRenderBackground(false);
        this.addRenderableWidget(campfireEffectWidget);

        int questHeight = 240;
        questWidget = new QuestWidget(minecraft,imageWidth / 2,questHeight,topPos+TAB_HEIGHT+20,topPos+questHeight -10,50);
        questWidget.setLeftPos(leftPos+5);
        questWidget.setRenderTopAndBottom(false);
        //campfireEffectWidget.setRenderBackground(false);
        this.addRenderableWidget(questWidget);

        activeQuestWidget = new ActiveQuestWidget(minecraft,imageWidth / 2,questHeight,topPos+TAB_HEIGHT+20,topPos+questHeight -10,50);
        activeQuestWidget.setLeftPos(leftPos+5);
        activeQuestWidget.setRenderTopAndBottom(false);
        //campfireEffectWidget.setRenderBackground(false);
        this.addRenderableWidget(activeQuestWidget);

        info = new Button(leftPos + imageWidth/2+76,topPos+imageHeight - 25,50,20,Component.literal("Info"),b -> moreInfo());
        addRenderableWidget(info);


        initEditBox();

        int threeBYPos = topPos + 165;

        //this.setInitialFocus(this.name);
        home = new ImageButton(leftPos+imageWidth/2 + 32,threeBYPos,16,16,0,0,0,TownCampfires.id("textures/gui/home.png"),16,16,
                b-> ForgePacketHandler.sendToServer(new C2STownCampfireButtonPacket(C2STownCampfireButtonPacket.CampfireButton.SPAWN,townCampfire.location())));
        addRenderableWidget(home);

        bed = new BedButton(leftPos+imageWidth/2 + 32+22,threeBYPos,20,20,Component.empty(),
                b-> ForgePacketHandler.sendToServer(new C2STownCampfireButtonPacket(C2STownCampfireButtonPacket.CampfireButton.BED,townCampfire.location())));
        addRenderableWidget(bed);

        gear = new ImageButton(leftPos+imageWidth/2 + 32+22 * 2,threeBYPos,20,20,0,0,0,TownCampfires.id("textures/gui/settings.png"),20,20,b->{});
        addRenderableWidget(gear);

        startQuest = new Button(leftPos + imageWidth/2+16,topPos+imageHeight - 25,60,20,Component.literal("Start Quest"),b -> pressStart());
        addRenderableWidget(startQuest);

        finishQuest = new Button(leftPos + imageWidth/2+16,topPos+imageHeight - 25,60,20,Component.literal("Finish"),b -> pressFinish());
        addRenderableWidget(finishQuest);

        questTabSwitch = new ExtendedButton(leftPos + 70,topPos+23,66,16,Component.literal("Available"),
                this::toggleQuestTab);
        addRenderableWidget(questTabSwitch);

        switchToTab(current);
    }

    void pressStart() {
        QuestWidget.QuestEntry selected = questWidget.getSelected();
        if (selected != null) {
            ForgePacketHandler.sendToServer(new C2STownCampfireQuestPacket(TownCampfiresClient.clientLookup(selected.quest),townCampfire.location(), C2STownCampfireQuestPacket.Type.START));
            startQuest.active = false;
        }
    }

    void pressFinish() {
        ActiveQuestWidget.ActiveQuestEntry selected = activeQuestWidget.getSelected();
        if (selected != null) {
            ForgePacketHandler.sendToServer(new C2STownCampfireQuestPacket(TownCampfiresClient.clientLookup(selected.questInstance.quest()),
                    townCampfire.location(), C2STownCampfireQuestPacket.Type.FINISH));
            finishQuest.active = false;
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
            Minecraft.getInstance().pushGuiLayer(new QuestInfoScreen(quest.name(),this,quest));
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

        int nameX = (imageWidth-nameWidth) / 2;

        this.name = new EditBox(this.font, leftPos + nameX +28, topPos + TAB_HEIGHT + 8, nameWidth, 12,
                Component.translatable("towncampfires.container.name"));
        this.name.setTextColor(0xeeeeee);
        this.name.setTextColorUneditable(0xeeeeee);
        this.name.setBordered(false);
        this.name.setMaxLength(40);
        this.name.setResponder(this::onNameChanged);
        this.addWidget(this.name);
    }

    private void onNameChanged(String string) {
        if (!string.isBlank()) {
            ForgePacketHandler.sendToServer(new C2SSetTownCampfireNamePacket(string,townCampfire.location()));
        }
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

    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        int xSize = imageWidth;
        int ySize = imageHeight - TAB_HEIGHT+4;
        RenderUtils.blitNineSlicedSized(pPoseStack,BACKGROUND,leftPos,topPos + TAB_HEIGHT,
                xSize,ySize,4,4,12,12,0,0,12,12);

    }

    public static final int DARK_GRAY = 0x404040;

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.toolTip = null;
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        this.name.render(pPoseStack, pMouseX, pMouseY, pPartialTick);


        if (townCampfire == null)return;

        int questCount = townCampfire.getAvailableQuests();

        switch (current) {
            case status -> {
                int y = 22;
                int h = 15;
                font.draw(pPoseStack,Component.literal("Villagers: "+townCampfire.getEffectiveVillagers()+"/"+townCampfire.getMaxVillagers()),
                        leftPos+8,topPos+TAB_HEIGHT+y,DARK_GRAY);
                font.draw(pPoseStack,Component.literal("Blocks Allowed: "+townCampfire.getUsedBlocks()+"/"+townCampfire.getAllowedBlocks()),
                        leftPos+8,topPos+TAB_HEIGHT+y + h,DARK_GRAY);

                font.draw(pPoseStack,Component.literal("Work Bench Limit: "+townCampfire.getUsedWorkbenches()+"/"+townCampfire.getAllowedWorkbenches()),
                        leftPos+8,topPos+TAB_HEIGHT+y + h*2,DARK_GRAY);

                font.draw(pPoseStack,Component.literal("Quests Available: "+questCount+"/"+townCampfire.getMaxQuests()),
                        leftPos+8,topPos+TAB_HEIGHT+y + h*3,DARK_GRAY);

                font.draw(pPoseStack,Component.literal("Refresh: "+TownCampfire.timeUntilRefresh(minecraft.level.getGameTime())),
                        leftPos+8,topPos+TAB_HEIGHT+y + h*4,DARK_GRAY);

                font.draw(pPoseStack,Component.literal("Raid Chance: "+"TODO"),
                        leftPos+8,topPos+TAB_HEIGHT+y + h*5,DARK_GRAY);
                renderExperience(pPoseStack, pMouseX, pMouseY, pPartialTick);

                font.draw(pPoseStack,Component.literal("Local Effects"),
                        leftPos+imageWidth/2f + 32,topPos+TAB_HEIGHT+y,DARK_GRAY);
            }
            case quest -> {

                font.draw(pPoseStack,Component.literal("Quests: "+questCount+"/"+townCampfire.getMaxQuests()),
                        leftPos+8,topPos+TAB_HEIGHT+5,DARK_GRAY);


                int spacing = 12;

                switch (questTab) {
                    case current -> {
                        ActiveQuestWidget.ActiveQuestEntry questEntry = activeQuestWidget.getSelected();
                        if (questEntry != null) {
                            QuestInstance questInstance = questEntry.questInstance;
                            Quest quest = questInstance.quest();
                            int xStart = leftPos + imageWidth/2 + 12;
                            int yStart = topPos+TAB_HEIGHT+4;

                            int yLine = 0;
                            itemRenderer.renderAndDecorateFakeItem(quest.icon(),xStart,yStart);

                            font.draw(pPoseStack, quest.name(),xStart+20,yStart,0xffffff);

                            List<FormattedCharSequence> seq = quest.desc().stream().map(Component::getVisualOrderText).toList();
                            for (int i = 0; i < seq.size();i++) {
                                FormattedCharSequence formattedCharSequence = seq.get(i);
                                font.draw(pPoseStack,formattedCharSequence,xStart + 1,yStart + 18 + spacing * i,0xffffff);
                            }
                            int completeY = 90;
                            font.draw(pPoseStack,Component.literal("Progress"),xStart,yStart+completeY,DARK_GRAY);
                            List<Pair<QuestCriteria<?>, Integer>> criterias = questInstance.quest().criterias();
                            for (int i = 0; i < criterias.size(); i++) {
                                Pair<QuestCriteria<?>, Integer> entry = criterias.get(i);
                                QuestCriteria<?> criteria = entry.getFirst();
                                int count = entry.getSecond();

                                Integer progress = questInstance.progress().isEmpty() ? 0 : questInstance.progress().get(i);

                                font.draw(pPoseStack,criteria.desc().copy().append(" "+progress+"/"+count),xStart,yStart + completeY +spacing +  spacing * i,0xffffff);
                            }

                            yLine = yStart+110+questInstance.quest().criterias().size()*spacing;

                            font.draw(pPoseStack,Component.literal("Members"),xStart,yLine,DARK_GRAY);

                            yLine+=spacing;

                            font.draw(pPoseStack,UsernameCache.getLastKnownUsername(questInstance.leader()),xStart,yLine,DARK_GRAY);

                            yLine+=spacing;

                            for (UUID uuid: questInstance.getMembers()) {
                                String name = UsernameCache.getLastKnownUsername(uuid);
                                font.draw(pPoseStack,name,xStart,yLine,DARK_GRAY);
                                yLine+= spacing;
                            }
                        }
                    }

                    case available -> {
                        QuestWidget.QuestEntry questEntry = questWidget.getSelected();
                        if (questEntry != null) {
                            Quest quest = questEntry.quest;
                            int xStart = leftPos + imageWidth/2 + 12;
                            int yStart = topPos+TAB_HEIGHT+4;
                            itemRenderer.renderAndDecorateFakeItem(quest.icon(),xStart,yStart);

                            font.draw(pPoseStack, quest.name(),xStart+20,yStart,0xffffff);

                            List<FormattedCharSequence> seq = quest.desc().stream().map(Component::getVisualOrderText).toList();
                            for (int i = 0; i < seq.size();i++) {
                                FormattedCharSequence formattedCharSequence = seq.get(i);
                                font.draw(pPoseStack,formattedCharSequence,xStart + 1,yStart + 18 + 10 * i,0xffffff);
                            }
                            int completeY = 90;
                            font.draw(pPoseStack,Component.literal("Complete Conditions"),xStart,yStart+completeY,DARK_GRAY);
                            List<Pair<QuestCriteria<?>, Integer>> criterias = quest.criterias();
                            for (int i = 0; i < criterias.size(); i++) {
                                Pair<QuestCriteria<?>, Integer> entry = criterias.get(i);
                                QuestCriteria<?> criteria = entry.getFirst();
                                int count = entry.getSecond();
                                font.draw(pPoseStack,criteria.desc().copy().append(" "+count),xStart,yStart + completeY +10 +  10 * i,0xffffff);
                            }
                        }
                    }
                }
            }
        }


        if (this.toolTip != null) {
            this.renderTooltip(pPoseStack, this.toolTip, pMouseX, pMouseY);
        }
    }

    void renderExperience(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
        int xPos = leftPos + imageWidth/ 2 - 91;
        int yPos = topPos + imageHeight - 22;
        int j = 182;
        long experience = townCampfire.getExperience();
        int experiencePerLevel = TownCampfireConfig.CONFIG.experience_per_level.get();

        float scale = (((float)experience % experiencePerLevel) / experiencePerLevel) * 183f;

        this.blit(pPoseStack, xPos, yPos, 0, 64, j, 5);
        if (scale > 0) {
            this.blit(pPoseStack, xPos, yPos, 0, 69, (int)scale, 5);
        }

        String level = "Level: "+townCampfire.getLevel();

        font.draw(pPoseStack,Component.literal(level),
                leftPos+ imageWidth/2f - font.width(level)/2f,yPos- 15,DARK_GRAY);

        String progress = (experience%experiencePerLevel)+"/"+experiencePerLevel;

        font.draw(pPoseStack,Component.literal(progress),
                leftPos+ imageWidth/2f - font.width(progress)/2f,yPos+ 10,DARK_GRAY);

    }

    boolean isEditboxActive() {
        return current == Tab.status;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }



    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.player.closeContainer();
        }

        return this.name.keyPressed(pKeyCode, pScanCode, pModifiers) || this.name.canConsumeInput() || super.keyPressed(pKeyCode, pScanCode, pModifiers);
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

         boolean status = isEditboxActive();
         name.setEditable(status);
         name.setVisible(status);
         home.visible = bed.visible = gear.visible = status;
         campfireEffectWidget.setVisible(status);

         questTabSwitch.visible = tab == Tab.quest;
         startQuest.visible = tab == Tab.quest && questWidget.getSelected() != null && isQuestAvailable(questWidget.getSelected().quest);

         handleQuestTabs(current == Tab.quest);
    }

    protected void handleQuestTabs(boolean visible) {
        questWidget.setVisible(visible && questTab == QuestTab.available);
        activeQuestWidget.setVisible(visible && questTab == QuestTab.current);
        questWidget.setSelected(null);
        activeQuestWidget.setSelected(null);
        startQuest.visible = visible && questTab==QuestTab.available && questWidget.getSelected() != null;
        finishQuest.visible = visible && questTab==QuestTab.current && activeQuestWidget.getSelected() != null;
        info.visible = visible && (questTab==QuestTab.current && (questWidget.getSelected() != null||activeQuestWidget.getSelected() != null));
    }

    public boolean isQuestAvailable(Quest quest) {
        ResourceLocation questID = TownCampfiresClient.clientLookup(quest);
        for (QuestInstance currentQuest : TownCampfiresClient.currentQuests) {
            ResourceLocation id = currentQuest.questID();
            if (Objects.equals(questID,id) && currentQuest.status().active) {
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

        public TabButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress, OnTooltip pOnTooltip,Tab tab) {
            super(pX, pY, pWidth, pHeight, pMessage, pOnPress, pOnTooltip);
            this.tab = tab;
        }

        @Override
        public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            boolean isSelected = tab == current;
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, BACKGROUND);
            RenderSystem.enableDepthTest();
            blitNineSlicedSizedTab(pPoseStack,BACKGROUND,x,y,
                    this.width,this.height + (isSelected ? 4 : 0),4,4,12,12,0,0,12,12);
            int j = getFGColor();
            drawCenteredString(pPoseStack, font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, j | Mth.ceil(this.alpha * 255.0F) << 24);

            if (this.isHovered) {
                this.renderToolTip(pPoseStack, pMouseX, pMouseY);
            }
        }

        public static void drawCenteredString(PoseStack pPoseStack, Font pFont, Component pText, int pX, int pY, int pColor) {
            FormattedCharSequence formattedcharsequence = pText.getVisualOrderText();
            pFont.draw(pPoseStack, formattedcharsequence, (float)(pX - pFont.width(formattedcharsequence) / 2), (float)pY, pColor);
        }



        /**
         * backport of 1.20 nineSlice
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
            blitNineSlicedSizedTab(stack,texture, x, y, width, height, sliceSize, sliceSize, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
        }

        public static void blitNineSlicedSizedTab(PoseStack stack,ResourceLocation texture, int x, int y, int width, int height, int sliceWidth,
                                                  int sliceHeight, int uWidth, int vHeight, int uOffset, int vOffset, int textureWidth, int textureHeight) {
            blitNineSlicedSizedTab(stack,texture, x, y, width, height, sliceWidth, sliceHeight, sliceWidth, sliceHeight, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
        }

        public static void blitNineSlicedSizedTab(PoseStack stack,ResourceLocation texture, int x, int y, int width, int height, int cornerWidth, int cornerHeight,
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
        list.forEach(location-> {
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
            buildList(townCampfire.getEffectIds(),this::addEntry, location->new CampfireEffectEntry(TownCampfiresClient.campfireEffectLoader.getCampfireEffects().get(location)));
        }

        @Override
        public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            if (visible) {
                //left,bottom,right,top
                GuiComponent.enableScissor(x0,y0,x1+10,y1);
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
                               int mouseX, int mouseY, boolean isMouseOver, float partialTick)
            {
                Font font = TownCampfireScreen.this.font;
                font.draw(poseStack,effect.name(),left,top,DARK_GRAY);

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
                startQuest.visible = true;
                info.visible = true;
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
            buildList(townCampfire.getQuestIds(),this::addEntry, location-> {
                if (TownCampfiresClient.isQuestAlreadyActive(location)) {
                    return null;
                }
                return new QuestEntry(TownCampfiresClient.questLoader.getQuestMap().get(location));
            });
        }

        @Override
        public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            if (visible) {
                //left,bottom,right,top
                GuiComponent.enableScissor(x0,y0,x1+10,y1);
                super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
                GuiComponent.disableScissor();
            }
        }

        @Override
        protected int getScrollbarPosition() {
            return x1;
        }

        public class QuestEntry extends ObjectSelectionList.Entry<QuestEntry> {
            private final Quest quest;

            QuestEntry(Quest info) {
                this.quest = info;
            }

            @Override
            public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
                if (visible) {
                    setSelected(this);
                    return true;
                }  else {
                    return false;
                }
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", quest.name());
            }

            @Override
            public void render(PoseStack poseStack, int entryIdx, int top, int left, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean isMouseOver, float partialTick)
            {
                Font font = TownCampfireScreen.this.font;
                itemRenderer.renderAndDecorateFakeItem(quest.icon(),left,top);
                font.draw(poseStack, quest.name(),left+20,top,0xffffff);

                    List<FormattedCharSequence> seq = quest.desc().stream().map(Component::getVisualOrderText).toList();
                    for (int i = 0; i < seq.size();i++) {
                        FormattedCharSequence formattedCharSequence = seq.get(i);
                        font.draw(poseStack,formattedCharSequence,left + 1,top + 18 + 10 * i,0xffffff);
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
                finishQuest.visible = true;
                info.visible = true;
                finishQuest.active = pSelected.questInstance.status() == QuestInstance.Status.COMPLETE;
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

        public void refreshList() {
            this.clearEntries();
            for (QuestInstance questInstance : TownCampfiresClient.currentQuests){
                addEntry(new ActiveQuestEntry(questInstance));
            }
        }

        @Override
        public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            if (visible) {
                //left,bottom,right,top
                GuiComponent.enableScissor(x0,y0,x1+10,y1);
                super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
                GuiComponent.disableScissor();
            }
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
                if (visible) {
                    setSelected(this);
                    return true;
                }  else {
                    return false;
                }
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", questInstance.quest().name());
            }

            @Override
            public void render(PoseStack poseStack, int entryIdx, int top, int left, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean isMouseOver, float partialTick)
            {
                Font font = TownCampfireScreen.this.font;
                itemRenderer.renderAndDecorateFakeItem(questInstance.quest().icon(),left,top);
                font.draw(poseStack, questInstance.quest().name(),left+20,top,0xffffff);

                List<FormattedCharSequence> seq = questInstance.quest().desc().stream().map(Component::getVisualOrderText).toList();
                for (int i = 0; i < seq.size();i++) {
                    FormattedCharSequence formattedCharSequence = seq.get(i);
                    font.draw(poseStack,formattedCharSequence,left + 1,top + 18 + 10 * i,0xffffff);
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
