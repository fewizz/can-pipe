package fewizz.canpipe.compat.xaero.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;

@Mixin(targets = "xaero.common.minimap.render.MinimapFBORenderer")
public class MinimapFBORendererMixin {

    @ModifyArg(
        method = "renderChunksToFBO",
        at = @At(value = "INVOKE", target = "Lxaero/lib/client/graphics/util/TextureUtils;clearRenderTarget"),
        index = 2
    )
    float changeClearDepth(float value) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null && !p.settings.awareOfDepthRangeChanges()) {
            value = 1.0F;
        }
        return value;
    }

}
