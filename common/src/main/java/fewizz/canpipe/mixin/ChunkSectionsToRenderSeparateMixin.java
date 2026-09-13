package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import fewizz.canpipe.pipeline.MaterialProgramLoader;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;

@Mixin(ChunkSectionsToRender.DrawIndirect.class)
public class ChunkSectionsToRenderSeparateMixin {

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
            pipeline == RenderPipelines.SOLID_TERRAIN ||
            pipeline == RenderPipelines.CUTOUT_TERRAIN ||
            pipeline == RenderPipelines.TRANSLUCENT_TERRAIN
        )) {
            MaterialProgramLoader loader = p.getMaterialProgramLoader(pipeline);
            if (loader != null) {
                return loader.getOrCompileRenderPipeline(p.solidFramebuffer.getFormats());
            }
        }
        return operation.call(pipeline);
    }

}
