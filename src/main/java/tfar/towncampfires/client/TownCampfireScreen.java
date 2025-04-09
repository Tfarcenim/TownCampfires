package tfar.towncampfires.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tfar.towncampfires.TownCampfires;

public class TownCampfireScreen extends Screen {

    public static final ResourceLocation BACKGROUND = TownCampfires.id("textures/gui/background.png");

    /** The X size of the inventory window in pixels. */
    protected int imageWidth = 200;
    /** The Y size of the inventory window in pixels. */
    protected int imageHeight = 166;
    /** Starting X position for the Gui. Inconsistent use for Gui backgrounds. */
    protected int leftPos;
    /** Starting Y position for the Gui. Inconsistent use for Gui backgrounds. */
    protected int topPos;

    public enum Tab {
        status,quest,trades,teleport,logs
    }

    protected static final int TAB_HEIGHT = 20;

    protected Tab current = Tab.status;

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
            addRenderableWidget(tabButton);
        }
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBg(pPoseStack, pPartialTick, pMouseX, pMouseY);
        RenderSystem.disableDepthTest();
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        int xSize = imageWidth;
        int ySize = imageHeight - TAB_HEIGHT+4;
        TabButton.blitNineSlicedSizedTab(pPoseStack,BACKGROUND,leftPos,topPos + TAB_HEIGHT,
                xSize,ySize,4,4,12,12,0,0,12,12);

    }

    protected void switchToTab(Tab tab) {
         current = tab;
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
        public static void blitNineSlicedSizedTab(PoseStack stack, ResourceLocation texture, int x, int y, int width, int height, int sliceSize, int uWidth, int vHeight, int uOffset, int vOffset, int textureWidth, int textureHeight) {
            blitNineSlicedSizedTab(stack,texture, x, y, width, height, sliceSize, sliceSize, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
        }

        public static void blitNineSlicedSizedTab(PoseStack stack,ResourceLocation texture, int x, int y, int width, int height, int sliceWidth, int sliceHeight, int uWidth, int vHeight, int uOffset, int vOffset, int textureWidth, int textureHeight) {
            blitNineSlicedSizedTab(stack,texture, x, y, width, height, sliceWidth, sliceHeight, sliceWidth, sliceHeight, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
        }

        public static void blitNineSlicedSizedTab(PoseStack stack,ResourceLocation texture, int x, int y, int width, int height, int cornerWidth, int cornerHeight, int edgeWidth, int edgeHeight, int uWidth, int vHeight, int uOffset, int vOffset, int textureWidth, int textureHeight) {
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
                blitRepeating(stack, x + cornerWidth, y, width - edgeWidth - cornerWidth, height, uOffset + cornerWidth, vOffset, uWidth - edgeWidth - cornerWidth, vHeight, textureWidth, textureHeight);
                GuiComponent.blit(stack, x + width - edgeWidth, y, (float) (uOffset + uWidth - edgeWidth), (float) vOffset, edgeWidth, height, textureWidth, textureHeight);
            } else if (width == uWidth) {
                GuiComponent.blit(stack, x, y, (float) uOffset, (float) vOffset, width, cornerHeight, textureWidth, textureHeight);
                blitRepeating(stack, x, y + cornerHeight, width, height - edgeHeight - cornerHeight, uOffset, vOffset + cornerHeight, uWidth, vHeight - edgeHeight - cornerHeight, textureWidth, textureHeight);
                GuiComponent.blit(stack, x, y + height - edgeHeight, (float) uOffset, (float) (vOffset + vHeight - edgeHeight), width, edgeHeight, textureWidth, textureHeight);
            } else {
                GuiComponent.blit(stack, x, y, (float) uOffset, (float) vOffset, cornerWidth, cornerHeight, textureWidth, textureHeight);
                blitRepeating(stack, x + cornerWidth, y, width - edgeWidth - cornerWidth, cornerHeight, uOffset + cornerWidth, vOffset, uWidth - edgeWidth - cornerWidth, cornerHeight, textureWidth, textureHeight);
                GuiComponent.blit(stack, x + width - edgeWidth, y, (float) (uOffset + uWidth - edgeWidth), (float) vOffset, edgeWidth, cornerHeight, textureWidth, textureHeight);

                //bottom left
                //GuiComponent.blit(stack, x, y + height - edgeHeight, (float) uOffset, (float) (vOffset + vHeight - edgeHeight), cornerWidth, edgeHeight, textureWidth, textureHeight);

                //bottom
                //blitRepeating(stack, x + cornerWidth, y + height - edgeHeight, width - edgeWidth - cornerWidth, edgeHeight, uOffset + cornerWidth, vOffset + vHeight - edgeHeight, uWidth - edgeWidth - cornerWidth, edgeHeight, textureWidth, textureHeight);

                //bottom right
                //GuiComponent.blit(stack, x + width - edgeWidth, y + height - edgeHeight, (float) (uOffset + uWidth - edgeWidth), (float) (vOffset + vHeight - edgeHeight), edgeWidth, edgeHeight, textureWidth, textureHeight);
                //left
                blitRepeating(stack, x, y + cornerHeight, cornerWidth, height - edgeHeight - cornerHeight, uOffset, vOffset + cornerHeight, cornerWidth, vHeight - edgeHeight - cornerHeight, textureWidth, textureHeight);
                //middle
                blitRepeating(stack, x + cornerWidth, y + cornerHeight, width - edgeWidth - cornerWidth, height - edgeHeight - cornerHeight, uOffset + cornerWidth, vOffset + cornerHeight, uWidth - edgeWidth - cornerWidth, vHeight - edgeHeight - cornerHeight, textureWidth, textureHeight);
                //right
                blitRepeating(stack, x + width - edgeWidth, y + cornerHeight, cornerWidth, height - edgeHeight - cornerHeight, uOffset + uWidth - edgeWidth, vOffset + cornerHeight, edgeWidth, vHeight - edgeHeight - cornerHeight, textureWidth, textureHeight);
            }

        }

        /**
         * Blits a repeating pattern of the texture specified by the atlas location onto the screen.
         * @param pX the x-coordinate of the top-left corner of the target position.
         * @param pY the y-coordinate of the top-left corner of the target position.
         * @param pWidth the width of the target portion.
         * @param pHeight the height of the target portion.
         * @param pUOffset the x-coordinate of the top-left corner of the source position.
         * @param pVOffset the y-coordinate of the top-left corner of the source position.
         * @param pSourceWidth the width of the source texture.
         * @param pSourceHeight the height of the source texture.
         */
        public static void blitRepeating(PoseStack matrices, int pX, int pY, int pWidth, int pHeight, int pUOffset, int pVOffset, int pSourceWidth, int pSourceHeight) {
            blitRepeating(matrices, pX, pY, pWidth, pHeight, pUOffset, pVOffset, pSourceWidth, pSourceHeight, 256, 256);
        }

        public static void blitRepeating(PoseStack matrices, int pX, int pY, int pWidth, int pHeight, int pUOffset, int pVOffset, int pSourceWidth, int pSourceHeight, int textureWidth, int textureHeight) {
            int i = pX;

            int j;
            for(IntIterator intiterator = slices(pWidth, pSourceWidth); intiterator.hasNext(); i += j) {
                j = intiterator.nextInt();
                int k = (pSourceWidth - j) / 2;
                int l = pY;

                int i1;
                for(IntIterator intiterator1 = slices(pHeight, pSourceHeight); intiterator1.hasNext(); l += i1) {
                    i1 = intiterator1.nextInt();
                    int j1 = (pSourceHeight - i1) / 2;
                    GuiComponent.blit(matrices, i, l, pUOffset + k, pVOffset + j1, j, i1, textureWidth, textureHeight);
                }
            }

        }

        /**
         * Returns an iterator for dividing a value into slices of a specified size.
         * <p>
         * @return An iterator for iterating over the slices.
         * @param pTarget the value to be divided.
         * @param pTotal the size of each slice.
         */
        private static IntIterator slices(int pTarget, int pTotal) {
            int i = Mth.positiveCeilDiv(pTarget, pTotal);
            return new Divisor(pTarget, i);
        }

    }
}
