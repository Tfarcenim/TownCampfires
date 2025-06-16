package tfar.towncampfires.mixin;

import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfar.towncampfires.TownCampfires;

import java.util.function.Predicate;

@Mixin(SimpleCriterionTrigger.class)
public class SimpleCriterionTriggerMixin<T extends AbstractCriterionTriggerInstance> {

    @Inject(method = "trigger",at = @At("RETURN"))
    private void checkQuestCriterion(ServerPlayer pPlayer, Predicate<T> pTestTrigger, CallbackInfo ci) {
        TownCampfires.checkQuestCriterion((SimpleCriterionTrigger<T>)(Object)this,pPlayer,pTestTrigger);
    }
}
