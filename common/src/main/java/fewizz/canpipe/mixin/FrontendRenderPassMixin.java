package fewizz.canpipe.mixin;

import java.util.List;
import java.util.Optional;

import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;

import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import fewizz.canpipe.renderpearl.PipelineCacheExtended;
import fewizz.canpipe.renderpearl.mixin.RenderSystemAccessor;
import net.minecraft.resources.Identifier;

@Mixin(FrontendRenderPass.class)
public class FrontendRenderPassMixin {

    @Shadow @Final private List<RenderPassDescriptor.@Nullable Attachment<Optional<Vector4fc>>> colorAttachments;

    @ModifyVariable(
        method = "setPipeline",
        at = @At("HEAD")
    )
    CompiledRenderPipeline onSetPipeline(CompiledRenderPipeline compiledRenderPipeline) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            PipelineCache pipelineCache = RenderSystemAccessor.canpipe_getCurrentPipelineCache();
            RenderPipeline renderPipeline = ((PipelineCacheExtended) pipelineCache).canpipe_getPipeline(compiledRenderPipeline);
            if (renderPipeline != null) {
                var newPipeline = p.onRenderPassSetPipeline(renderPipeline, this.colorAttachments);
                if (newPipeline != null) {
                    compiledRenderPipeline = newPipeline;
                }
            }
        }
        return compiledRenderPipeline;
    }

}
