package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import fewizz.canpipe.GFX;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    void onInitEnd(CallbackInfo ci) {
        GFX.glEnableCubemapSeamless();
    }

    @ModifyReturnValue(method = "useShaderTransparency", at = @At("RETURN"))
    private static boolean useShaderTransparency(boolean original) {
        return original || Pipelines.getCurrent() != null;
    }

}
