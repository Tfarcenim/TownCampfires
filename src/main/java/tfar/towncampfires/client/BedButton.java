package tfar.towncampfires.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BedButton extends Button {
    public BedButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress) {
        super(pX, pY, pWidth, pHeight, pMessage, pOnPress);
    }

    public BedButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress, OnTooltip pOnTooltip) {
        super(pX, pY, pWidth, pHeight, pMessage, pOnPress, pOnTooltip);
    }

    @Override
    public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        RenderSystem.enableDepthTest();

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        itemRenderer.renderAndDecorateFakeItem(new ItemStack(Items.RED_BED),x,y);

        int j = getFGColor();
       // drawCenteredString(pPoseStack, font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, j | Mth.ceil(this.alpha * 255.0F) << 24);

        if (this.isHovered) {
            this.renderToolTip(pPoseStack, pMouseX, pMouseY);
        }
    }
}
