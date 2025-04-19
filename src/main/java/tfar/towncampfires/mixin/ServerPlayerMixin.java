package tfar.towncampfires.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfar.towncampfires.TownCampfires;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Shadow public abstract ServerLevel getLevel();

    @Inject(method = "isReachableBedBlock",at = @At("HEAD"),cancellable = true)
    private void disableRangeCheck(BlockPos bedPos, CallbackInfoReturnable<Boolean> cir) {
        if (TownCampfires.checkBed((ServerPlayer)(Object) this,bedPos)) {
            cir.setReturnValue(true);
        }
    }
}
