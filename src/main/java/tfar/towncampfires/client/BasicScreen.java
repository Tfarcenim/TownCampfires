package tfar.towncampfires.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import tfar.towncampfires.TownCampfires;

public class BasicScreen extends Screen {

    public static final ResourceLocation BACKGROUND = TownCampfires.id("textures/gui/background.png");
    /** The X size of the inventory window in pixels. */
    protected int imageWidth = 320;
    /** The Y size of the inventory window in pixels. */
    protected int imageHeight = 230;
    /** Starting X position for the Gui. Inconsistent use for Gui backgrounds. */
    protected int leftPos;
    /** Starting Y position for the Gui. Inconsistent use for Gui backgrounds. */
    protected int topPos;

    protected int titleLabelX;
    protected int titleLabelY;

    protected BasicScreen(Component pTitle) {
        super(pTitle);
        titleLabelX = 6;
        titleLabelY = 8;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBg(pPoseStack, pPartialTick, pMouseX, pMouseY);
        RenderSystem.disableDepthTest();
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        this.renderLabels(pPoseStack, pMouseX, pMouseY);
    }

    protected void renderLabels(PoseStack pPoseStack, int pMouseX, int pMouseY) {

    }

    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
    }


}
