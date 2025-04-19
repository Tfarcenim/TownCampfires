package tfar.towncampfires.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.darkhax.bookshelf.api.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import tfar.towncampfires.TownCampfire;
import tfar.towncampfires.TownCampfireConfig;
import tfar.towncampfires.TownCampfires;
import tfar.towncampfires.network.ForgePacketHandler;
import tfar.towncampfires.network.server.C2SSetTownCampfireNamePacket;
import tfar.towncampfires.network.server.C2STownCampfireButtonPacket;

public class TownCampfireScreen extends Screen {

    public static final ResourceLocation BACKGROUND = TownCampfires.id("textures/gui/background.png");

    protected EditBox name;

    /** The X size of the inventory window in pixels. */
    protected int imageWidth = 320;
    /** The Y size of the inventory window in pixels. */
    protected int imageHeight = 230;
    /** Starting X position for the Gui. Inconsistent use for Gui backgrounds. */
    protected int leftPos;
    /** Starting Y position for the Gui. Inconsistent use for Gui backgrounds. */
    protected int topPos;
    private TownCampfire townCampfire;

    public void setCampfire(TownCampfire townCampfire) {
        this.townCampfire = townCampfire;
        name.setValue(townCampfire.name().getString());
    }

    public enum Tab {
        status,quest,trades,teleport,logs
    }

    protected static final int TAB_HEIGHT = 20;

    protected Tab current = Tab.status;
    protected Button home;
    protected Button bed;
    protected Button gear;

    protected TownCampfireScreen(Component pTitle) {
        super(pTitle);
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        Tab[] values = Tab.values();
        int tabWidth = imageWidth / values.length;
        for (int i = 0; i < values.length; i++) {
            Tab tab = values[i];
            int xPos = leftPos + i * tabWidth;
            TabButton tabButton = new TabButton(xPos,topPos,tabWidth,TAB_HEIGHT+4,Component.literal(tab.name()),pButton -> switchToTab(tab),tab);
            tabButton.setFGColor(0x404040);
            addRenderableWidget(tabButton);
        }
        initEditBox();
        //this.setInitialFocus(this.name);
        home = new ImageButton(leftPos+imageWidth/2 + 32,topPos+126,16,16,0,0,0,TownCampfires.id("textures/gui/home.png"),16,16,
                b-> ForgePacketHandler.sendToServer(new C2STownCampfireButtonPacket(C2STownCampfireButtonPacket.CampfireButton.SPAWN,townCampfire.location())));
        addRenderableWidget(home);

        bed = new BedButton(leftPos+imageWidth/2 + 32+22,topPos+126,20,20,Component.empty(),
                b-> ForgePacketHandler.sendToServer(new C2STownCampfireButtonPacket(C2STownCampfireButtonPacket.CampfireButton.BED,townCampfire.location())));
        addRenderableWidget(bed);

        gear = new ImageButton(leftPos+imageWidth/2 + 32+22 * 2,topPos+126,20,20,0,0,0,TownCampfires.id("textures/gui/settings.png"),20,20,b->{});
        addRenderableWidget(gear);

        switchToTab(current);
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

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBg(pPoseStack, pPartialTick, pMouseX, pMouseY);
        RenderSystem.disableDepthTest();
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        this.name.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

        if (townCampfire == null)return;
        switch (current) {
            case status -> {
                int y = 20;
                int h = 15;
                font.draw(pPoseStack,Component.literal("Villagers: "+townCampfire.getEffectiveVillagers()+"/"+townCampfire.getMaxVillagers()),
                        leftPos+8,topPos+TAB_HEIGHT+y,0x404040);
                font.draw(pPoseStack,Component.literal("Blocks Allowed: "+townCampfire.getUsedBlocks()+"/"+townCampfire.getAllowedBlocks()),
                        leftPos+8,topPos+TAB_HEIGHT+y + h,0x404040);

                font.draw(pPoseStack,Component.literal("Work Bench Limit: "+townCampfire.getUsedWorkbenches()+"/"+townCampfire.getAllowedWorkbenches()),
                        leftPos+8,topPos+TAB_HEIGHT+y + h*2,0x404040);

                font.draw(pPoseStack,Component.literal("Quests Available: "+"TODO"+"/"+"TODO"),
                        leftPos+8,topPos+TAB_HEIGHT+y + h*3,0x404040);

                font.draw(pPoseStack,Component.literal("Refresh: "+TownCampfire.timeUntilRefresh(minecraft.level.getGameTime())),
                        leftPos+8,topPos+TAB_HEIGHT+y + h*4,0x404040);

                font.draw(pPoseStack,Component.literal("Raid Chance: "+"TODO"),
                        leftPos+8,topPos+TAB_HEIGHT+y + h*5,0x404040);
                renderExperience(pPoseStack, pMouseX, pMouseY, pPartialTick);

                font.draw(pPoseStack,Component.literal("Local Effects"),
                        leftPos+imageWidth/2f + 32,topPos+TAB_HEIGHT+y,0x404040);
            }
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
                leftPos+ imageWidth/2f - font.width(level)/2f,yPos- 15,0x404040);

        String progress = (experience%experiencePerLevel)+"/"+experiencePerLevel;

        font.draw(pPoseStack,Component.literal(progress),
                leftPos+ imageWidth/2f - font.width(progress)/2f,yPos+ 10,0x404040);

    }

    boolean isEditboxActive() {
        return current == Tab.status;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        int xSize = imageWidth;
        int ySize = imageHeight - TAB_HEIGHT+4;
        RenderUtils.blitNineSlicedSized(pPoseStack,BACKGROUND,leftPos,topPos + TAB_HEIGHT,
                xSize,ySize,4,4,12,12,0,0,12,12);

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

    protected void switchToTab(Tab tab) {
         current = tab;
         boolean visible = isEditboxActive();
         name.setEditable(visible);
         name.setVisible(visible);
         home.visible = bed.visible = gear.visible = visible;
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
}
