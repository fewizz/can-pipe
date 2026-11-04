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

import fewizz.canpipe.pipeline.MaterialProgramProvider;
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
            MaterialProgramProvider prover = p.getMaterialProgramProvider(pipeline);
            if (prover != null) {
                var colorAttachments = ((FrontendRenderPassAccessor) renderPass).canpipe_getColorAttachments();
                var colorFormats = colorAttachments.stream().map(cts -> cts.textureView().texture().getFormat()).toList();
                var depthFormat = GpuFormat.D32_FLOAT;
                return prover.getOrCreateCompiledRenderPipeline(Pair.of(colorFormats, depthFormat));
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
            MaterialProgramProvider provider = p.getMaterialProgramProvider(renderPipeline);
            if (provider != null) {
                Pipeline.bindSpritesExtentsSampler(renderPass, p.targets.solid().getColorTextureView());
                p.bindUniforms(renderPass, p.targets.solid(), true);
            }
        }
    }

}
