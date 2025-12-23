package tfar.towncampfires.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class SmallButton extends Button {
    public SmallButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress) {
        super(pX, pY, pWidth, pHeight, pMessage, pOnPress);
    }

    public SmallButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress, OnTooltip pOnTooltip) {
        super(pX, pY, pWidth, pHeight, pMessage, pOnPress, pOnTooltip);
    }
}
