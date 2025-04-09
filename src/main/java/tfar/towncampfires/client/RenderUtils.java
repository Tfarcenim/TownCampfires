package tfar.towncampfires.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RenderUtils {

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
    public static void blitNineSlicedSized(PoseStack stack,ResourceLocation texture, int x, int y, int width, int height, int sliceSize, int uWidth, int vHeight, int uOffset, int vOffset, int textureWidth, int textureHeight) {
        blitNineSlicedSized(stack,texture, x, y, width, height, sliceSize, sliceSize, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
    }

    public static void blitNineSlicedSized(PoseStack stack,ResourceLocation texture, int x, int y, int width, int height, int sliceWidth, int sliceHeight, int uWidth, int vHeight, int uOffset, int vOffset, int textureWidth, int textureHeight) {
        blitNineSlicedSized(stack,texture, x, y, width, height, sliceWidth, sliceHeight, sliceWidth, sliceHeight, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
    }

    public static void blitNineSlicedSized(PoseStack stack,ResourceLocation texture, int x, int y, int width, int height, int cornerWidth, int cornerHeight, int edgeWidth, int edgeHeight, int uWidth, int vHeight, int uOffset, int vOffset, int textureWidth, int textureHeight) {
        cornerWidth = Math.min(cornerWidth, width / 2);
        edgeWidth = Math.min(edgeWidth, width / 2);
        cornerHeight = Math.min(cornerHeight, height / 2);
        edgeHeight = Math.min(edgeHeight, height / 2);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, texture);
        Gui self = Minecraft.getInstance().gui;
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
            GuiComponent.blit(stack, x, y + height - edgeHeight, (float) uOffset, (float) (vOffset + vHeight - edgeHeight), cornerWidth, edgeHeight, textureWidth, textureHeight);
            blitRepeating(stack, x + cornerWidth, y + height - edgeHeight, width - edgeWidth - cornerWidth, edgeHeight, uOffset + cornerWidth, vOffset + vHeight - edgeHeight, uWidth - edgeWidth - cornerWidth, edgeHeight, textureWidth, textureHeight);
            GuiComponent.blit(stack, x + width - edgeWidth, y + height - edgeHeight, (float) (uOffset + uWidth - edgeWidth), (float) (vOffset + vHeight - edgeHeight), edgeWidth, edgeHeight, textureWidth, textureHeight);
            blitRepeating(stack, x, y + cornerHeight, cornerWidth, height - edgeHeight - cornerHeight, uOffset, vOffset + cornerHeight, cornerWidth, vHeight - edgeHeight - cornerHeight, textureWidth, textureHeight);
            blitRepeating(stack, x + cornerWidth, y + cornerHeight, width - edgeWidth - cornerWidth, height - edgeHeight - cornerHeight, uOffset + cornerWidth, vOffset + cornerHeight, uWidth - edgeWidth - cornerWidth, vHeight - edgeHeight - cornerHeight, textureWidth, textureHeight);
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
