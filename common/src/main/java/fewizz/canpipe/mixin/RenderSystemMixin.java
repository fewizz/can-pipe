package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;

import fewizz.canpipe.pipeline.MaterialProgramLoader;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    /*@Inject(
        method = "getCompiledPipelineNullable",
        at = @At("HEAD"),
        cancellable = true
    )
    void onGetCompiledPipelineNullable(RenderPipeline pipeline, CallbackInfoReturnable<CompiledRenderPipeline> ci) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            MaterialProgramLoader loader = p.getMaterialProgramLoader(pipeline);
            if (loader != null) {
                loader.
            }
            ci.cancel();
        }
    }*/

}
