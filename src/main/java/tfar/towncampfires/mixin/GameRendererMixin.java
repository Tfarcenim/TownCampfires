package tfar.towncampfires.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfar.towncampfires.client.TownCampfiresClient;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "tryTakeScreenshotIfNeeded",at = @At("RETURN"))
    private void campfireScreenshot(CallbackInfo ci) {
        TownCampfiresClient.takeCampfireScreenshotIfNeeded((GameRenderer)(Object)this);
    }
}
