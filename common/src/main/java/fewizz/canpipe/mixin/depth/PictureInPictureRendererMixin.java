package fewizz.canpipe.mixin.depth;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;

@Mixin(PictureInPictureRenderer.class)
public class PictureInPictureRendererMixin {

    @ModifyConstant(method = "prepareTexturesAndProjection", constant = @Constant(doubleValue = 0.0))
    double replaceDepthClearValue(double value) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null && !p.awareOfDepthRangeChanges) {
            value = 1.0;
        }
        return value;
    }

}
