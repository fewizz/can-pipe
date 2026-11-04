package fewizz.canpipe.mixin;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import fewizz.canpipe.renderpearl.mixin.FrontendRenderPassAccessor;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;

@Mixin(PreparedRenderType.class)
public class PreparedRenderTypeMixin {

    @WrapOperation(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;getCompiledPipeline("+
                "Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"+
            ")Lcom/mojang/renderpearl/api/pipeline/CompiledRenderPipeline;"
        )
    )
    CompiledRenderPipeline replaceRenderPipeline(
        RenderPipeline pipeline,
        Operation<CompiledRenderPipeline> operation,
        @Local RenderPass renderPass
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            var loader = p.getMaterialProgramLoader(pipeline);
            if (loader != null) {
                var colorAttachments = ((FrontendRenderPassAccessor) renderPass).canpipe_getColorAttachments();
                var colorFormats = colorAttachments.stream().map(cts -> cts.textureView().texture().getFormat()).toList();
                var depthFormat = GpuFormat.D32_FLOAT;
                return loader.getOrCreateCompiledRenderPipeline(Pair.of(colorFormats, depthFormat));
            }
        }
        return operation.call(pipeline);
    }

    @Inject(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;bindDefaultUniforms(Lcom/mojang/renderpearl/api/commands/RenderPass;)V"
        )
    )
    void bindUniforms(CallbackInfo ci, @Local RenderPipeline renderPipeline, @Local RenderPass renderPass) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            var loader = p.getMaterialProgramLoader(renderPipeline);
            if (loader != null) {
                Pipeline.bindSpritesExtentsSampler(renderPass, p.targets.solid().getColorTextureView());
                p.bindUniforms(renderPass, p.targets.solid(), true);
            }
        }
    }

}
