package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderTarget;

import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.WeatherEffectRenderer;

@Mixin(WeatherEffectRenderer.class)
public class WeatherEffectRendererMixin {

    @ModifyExpressionValue(
        method = {
            "render("+
                "Lnet/minecraft/world/phys/Vec3;"+
                "Lnet/minecraft/client/renderer/state/level/WeatherRenderState;"+
            ")V",
            "render("+  // NEO
                "Lnet/minecraft/world/phys/Vec3;"+
                "Lnet/minecraft/client/renderer/state/level/WeatherRenderState;"+
                "Lnet/minecraft/client/renderer/state/level/LevelRenderState;"+
            ")V"
        },
        require = 1,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/OutputTarget;getRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"
        )
    )
    RenderTarget replaceWeatherRenderTarget(RenderTarget renderTarget) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            renderTarget = p.shadowFramebufferOr(p.weatherFramebuffer);
        }
        return renderTarget;
    }

}
