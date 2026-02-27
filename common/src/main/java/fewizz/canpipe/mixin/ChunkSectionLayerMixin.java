package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

@Mixin(ChunkSectionLayer.class)
public class ChunkSectionLayerMixin {

    @ModifyReturnValue(method = "pipeline", at = @At("RETURN"))
    public RenderPipeline pipeline(RenderPipeline renderPipeline) {
        Pipeline p = Pipelines.getCurrent();

        if (p != null) {
            Minecraft mc = Minecraft.getInstance();
            renderPipeline =
                !((LevelRendererExtended) mc.levelRenderer).canpipe_getIsRenderingShadows()
                ? p.materialPrograms.get(renderPipeline)
                : p.shadows.materialPrograms().get(renderPipeline);
        }

        return renderPipeline;
    }

    @ModifyReturnValue(method = "vertexFormat", at = @At("RETURN"))
    public VertexFormat vertexFormat(VertexFormat vertexFormat) {
        Pipeline p = Pipelines.getCurrent();

        if (p != null) {
            Minecraft mc = Minecraft.getInstance();
            boolean shadow = ((LevelRendererExtended) mc.levelRenderer).canpipe_getIsRenderingShadows();

            if (vertexFormat == DefaultVertexFormat.BLOCK) {
                vertexFormat = CanPipe.VertexFormats.BLOCK;
            }
            else if (vertexFormat == DefaultVertexFormat.ENTITY) {
                vertexFormat = !shadow ? CanPipe.VertexFormats.ENTITY : CanPipe.VertexFormats.ENTITY_SHADOW;
            }
            else if (vertexFormat == DefaultVertexFormat.PARTICLE) {
                vertexFormat = CanPipe.VertexFormats.PARTICLE;
            }
            else {
                throw new RuntimeException("Unexpected vertex format "+vertexFormat);
            }
        }

        return vertexFormat;
    }

}
