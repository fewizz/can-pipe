package fewizz.canpipe.pipeline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import fewizz.canpipe.b3d.RenderPipelineBuilderExtended;
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
                postfix += "-c-"+colorAttachmentFormat.toString();
            }
            if (formats.getRight() != null) {
                postfix += "-d-"+formats.getRight().toString();
            }
            postfix = postfix.replace("_", "-").toLowerCase();

            pipelineBuilder.withLocation(this.id.withSuffix(postfix));

            var originalBlendFunction = this.originalRenderPipeline.getColorTargetState().blendFunction();

            for (int i = 0; i < colorFormats.size(); ++i) {
                pipelineBuilder.withColorTargetState(i, new ColorTargetState(originalBlendFunction, colorFormats.get(i), ColorTargetState.WRITE_ALL));
            }

            return pipelineBuilder.build();
        });
    }

}
