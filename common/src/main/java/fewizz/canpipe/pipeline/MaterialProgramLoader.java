package fewizz.canpipe.pipeline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.b3d.RenderPipelineBuilderExtended;
import fewizz.canpipe.mixin.RenderPipelineBuilderAccessor;
import net.minecraft.resources.Identifier;

public class MaterialProgramLoader {

    private final Identifier id;
    private final RenderPipeline.Builder pipelineBuilder;
    private final RenderPipeline originalRenderPipeline;
    private final Map<Pair<List<GpuFormat>, GpuFormat>, RenderPipeline> renderPipelinesPerFormat = new HashMap<>();

    MaterialProgramLoader(Identifier id, RenderPipeline.Builder pipelineBuilder, RenderPipeline originalRenderPipeline) {
        this.id = id;
        this.pipelineBuilder = pipelineBuilder;
        this.originalRenderPipeline = originalRenderPipeline;
    }

    public RenderPipeline getOrCompileRenderPipeline(Pair<List<GpuFormat>, GpuFormat> formats) {
        return this.renderPipelinesPerFormat.computeIfAbsent(formats, (_formats) -> {
            ((RenderPipelineBuilderExtended) this.pipelineBuilder).canpipe_resetActiveColorTargetStateCount();
            var colorFormats = formats.getLeft();
            // var depthFormat = formats.getRight();

            String postfix = "";
            for (var colorAttachmentFormat : formats.getLeft()) {
                if (colorAttachmentFormat != null) {
                    postfix += "-c-"+colorAttachmentFormat.toString();
                }
                else {
                    postfix += "-c-u";  // unused
                }
            }
            if (formats.getRight() != null) {
                postfix += "-d-"+formats.getRight().toString();
            }
            postfix = postfix.replace("_", "-").toLowerCase();

            pipelineBuilder.withLocation(this.id.withSuffix(postfix));

            var originalBlendFunction = this.originalRenderPipeline.getColorTargetState().blendFunction();

            for (int i = 0; i < colorFormats.size(); ++i) {
                var format = colorFormats.get(i);
                if (format != null) {
                    boolean blend = !(
                        format.componentType() == GpuFormat.ComponentType.SINT_8 ||
                        format.componentType() == GpuFormat.ComponentType.UINT_8 ||
                        format.componentType() == GpuFormat.ComponentType.SINT_16 ||
                        format.componentType() == GpuFormat.ComponentType.UINT_16 ||
                        format.componentType() == GpuFormat.ComponentType.SINT_32 ||
                        format.componentType() == GpuFormat.ComponentType.UINT_32
                    );
                    pipelineBuilder.withColorTargetState(
                        i,
                        new ColorTargetState(blend ? originalBlendFunction : Optional.empty(), format, ColorTargetState.WRITE_ALL)
                    );
                }
                else {
                    pipelineBuilder.withUnusedColorTargetState(i);
                }
            }

            return pipelineBuilder.build();
        });
    }

    public VertexFormat vertexFormat() {
        return ((RenderPipelineBuilderAccessor) this.pipelineBuilder).canpipe_getVertexFormatPerBuffer()[0];
    }

}
