package fewizz.canpipe.compat.hdrmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fewizz.canpipe.compat.hdrmod.HDRModCompat;
import net.minecraft.world.InteractionResult;
import xyz.rrtt217.HDRMod.HDRMod;

@Mixin(HDRMod.class)
public class HDRModMixin {

    @Inject(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lme/shedaniel/autoconfig/ConfigHolder;getConfig"
        )
    )
    private static void registerSaveListener(CallbackInfo ci) {
        HDRMod.configHolder.registerSaveListener((configHolder, config) -> {
            HDRModCompat.updatePipelineIfHDROptionValueChanged();
            return InteractionResult.PASS;
        });
    }

}
