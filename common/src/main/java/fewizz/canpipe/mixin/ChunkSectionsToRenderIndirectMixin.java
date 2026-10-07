package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.pipeline.MaterialProgramLoader;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;

@Mixin(ChunkSectionsToRender.DrawIndirect.class)
public class ChunkSectionsToRenderIndirectMixin {

    @WrapOperation(
        method = "render",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getCompiledPipeline")
    )
    CompiledRenderPipeline onGetCompiledPipeline(
        RenderPipeline pipeline,
        Operation<CompiledRenderPipeline> operation
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null && (
            pipeline == RenderPipelines.SOLID_TERRAIN_MULTIDRAW ||
            pipeline == RenderPipelines.CUTOUT_TERRAIN_MULTIDRAW ||
            pipeline == RenderPipelines.TRANSLUCENT_TERRAIN_MULTIDRAW
        )) {
            MaterialProgramLoader loader = p.getMaterialProgramLoader(pipeline);
            if (loader != null) {
                return loader.getOrCompileRenderPipeline(p.solidFramebuffer.getFormats());
            }
        }
        return operation.call(pipeline);
    }

    @Inject(
        method = "render",
        at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/RenderPass;setPipeline", shift = Shift.AFTER)
    )
    void onSetPipeline(CallbackInfo ci, @Local RenderPass renderPass) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            Pipeline.bindSpritesExtentsSampler(renderPass, p.solidFramebuffer.getColorTextureView());
            p.bindUniforms(renderPass, p.solidFramebuffer, false);
        }
    }

    @ModifyConstant (
        method = "render",
        constant = @Constant(longValue = 20)
    )
    long changeVertexSize(long size) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            // size = CanPipe.VertexFormats.BLOCK.getVertexSize();
        }
        return size;
    }

}
