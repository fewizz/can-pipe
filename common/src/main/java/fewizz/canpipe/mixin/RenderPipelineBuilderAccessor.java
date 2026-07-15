package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;

@Mixin(RenderPipeline.Builder.class)
public interface RenderPipelineBuilderAccessor {

    @Accessor("vertexFormatPerBuffer") VertexFormat[] canpipe_getVertexFormatPerBuffer();

}
