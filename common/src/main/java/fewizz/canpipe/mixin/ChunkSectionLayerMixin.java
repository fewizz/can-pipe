package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.MaterialProgramLoader;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

@Mixin(ChunkSectionLayer.class)
public class ChunkSectionLayerMixin {

    @Final private boolean translucent;

    @ModifyReturnValue(method = "pipeline", at = @At("RETURN"))
    public RenderPipeline pipeline(RenderPipeline renderPipeline) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            MaterialProgramLoader loader = p.getMaterialProgramLoader(renderPipeline);
            Framebuffer fb = p.shadowFramebufferOr(this.translucent ? p.translucentTerrainFramebuffer : p.solidFramebuffer);
            var formats = fb.getFormats();
            renderPipeline = loader.getOrCompileRenderPipeline(formats);
        }
        return renderPipeline;
    }

    @ModifyReturnValue(method = "vertexFormat", at = @At("RETURN"))
    public VertexFormat vertexFormat(VertexFormat vertexFormat) {
        Pipeline p = Pipelines.getCurrent();

        if (p != null) {
            if (vertexFormat == DefaultVertexFormat.BLOCK) {
                vertexFormat = CanPipe.VertexFormats.BLOCK;
            }
            else {
                throw new RuntimeException("Unexpected vertex format "+vertexFormat);
            }
        }

        return vertexFormat;
    }

}
