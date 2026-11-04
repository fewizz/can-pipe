package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.renderpearl.api.vertex.VertexFormat;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

@Mixin(SectionRenderDispatcher.class)
public class SectionRenderDispatcherMixin {

    @ModifyExpressionValue(
        method = "lambda$new$0",
        at = @At(
            value = "INVOKE",
            target = "com/mojang/renderpearl/api/pipeline/RenderPipeline.getVertexFormatBinding(I)Lcom/mojang/renderpearl/api/vertex/VertexFormat;"
        )
    )
    VertexFormat onWriteChunkSectionCommands(VertexFormat terrainVertexFormat) {
        var p = Pipelines.getCurrent();
        if (p != null) {
            terrainVertexFormat = CanPipe.VertexFormats.BLOCK;
        }
        return terrainVertexFormat;
    }

}
