package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

@Mixin(RenderType.class)
public class RenderTypeMixin {

    @Shadow @Final private RenderSetup state;

    @ModifyReturnValue(
        method = "format",
        at = @At("RETURN")
    )
    VertexFormat replaceRenderPipeline(VertexFormat format) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            var originalRenderPipeline = ((RenderSetupAccessor) (Object) this.state).canpipe_getPipeline();
            var loader = p.getMaterialProgramLoader(originalRenderPipeline);
            if (loader != null) {  // TODO
                format = loader.vertexFormat();
            }
        }
        return format;
    }

}
