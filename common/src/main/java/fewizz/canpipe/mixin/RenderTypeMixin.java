package fewizz.canpipe.mixin;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;

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
            if (loader != null) {
                format = loader.vertexFormat();
            }
        }
        return format;
    }

    /*@ModifyArg(
        method = "prepare",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/PreparedRenderType;<init>"
        )
    )
    RenderPipeline replaceRenderPipeline(RenderPipeline renderPipeline) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            var loader = p.getMaterialProgramLoader(renderPipeline);
            if (loader != null) {
                var colorFormats = renderPipeline.getColorTargetStates().stream().map(cts -> cts.format()).toList();
                var depthFormat = GpuFormat.D32_FLOAT;
                renderPipeline = loader.createRenderPipeline(Pair.of(colorFormats, depthFormat));
            }
        }
        return renderPipeline;
    }*/

}
