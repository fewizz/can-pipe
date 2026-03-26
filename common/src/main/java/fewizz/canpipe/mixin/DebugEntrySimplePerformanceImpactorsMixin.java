package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.gui.components.debug.DebugEntrySimplePerformanceImpactors;

@Mixin(DebugEntrySimplePerformanceImpactors.class)
public class DebugEntrySimplePerformanceImpactorsMixin {

    // Because we don't change this value, only overriding it

    @ModifyExpressionValue(
        method = "display",
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;textureFiltering()Lnet/minecraft/client/OptionInstance;")
        ),
        at = @At(
            value = "INVOKE",
            ordinal = 0,
            target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"
        )
    )
    Object fixTextureFilteringInfo(Object value) {
        if (Pipelines.getCurrent() != null) {
            value = TextureFilteringMethod.NONE;
        }
        return value;
    }

}
