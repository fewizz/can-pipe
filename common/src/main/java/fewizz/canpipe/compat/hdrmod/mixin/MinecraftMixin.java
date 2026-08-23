package fewizz.canpipe.compat.hdrmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fewizz.canpipe.compat.hdrmod.HDRModCompat;
import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;initRenderer(Lcom/mojang/blaze3d/systems/GpuDevice;)V",
            shift = Shift.AFTER
        )
    )
    void afterRendererInit(CallbackInfo ci) {
        HDRModCompat.updatePipelineIfHDROptionValueChanged();
    }

}
