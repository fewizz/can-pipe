package fewizz.canpipe.mixin;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.pipeline.MaterialProgramLoader;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

@Mixin(SectionRenderDispatcher.class)
public class SectionRenderDispatcherMixin {

    @ModifyExpressionValue(
        method = "lambda$new$0",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/client/renderer/chunk/ChunkSectionLayer.pipeline(Z)Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"
        )
    )
    RenderPipeline onWriteChunkSectionCommands(RenderPipeline pipeline) {
        var p = Pipelines.getCurrent();
        if (p != null) {
            MaterialProgramLoader programLoader = p.getMaterialProgramLoader(pipeline);
            pipeline = programLoader.createRenderPipeline(Pair.of(List.of(), GpuFormat.D32_FLOAT));
        }
        return pipeline;
    }

}
