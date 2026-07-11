package fewizz.canpipe.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassDescriptor;

import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;

@Mixin(RenderPass.class)
public class RenderPassMixin {

    @Shadow @Final private List<RenderPassDescriptor.Attachment<Optional<Vector4fc>>> colorAttachments;

    @ModifyVariable(
        method = "setPipeline",
        at = @At("HEAD")
    )
    RenderPipeline onSetPipeline(RenderPipeline pipeline) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) { return pipeline; }

        if (
            !pipeline.getLocation().getNamespace().equals("minecraft")
            || this.colorAttachments.get(0).textureView().texture().getFormat() == GpuFormat.RGBA8_UNORM
        ) {
            return pipeline;
        }

        List<GpuFormat> colorAttachmentFormats = new ArrayList<>();
        for (var attachment : this.colorAttachments) {
            colorAttachmentFormats.add(attachment.textureView().texture().getFormat());
        }

        return p.replacedRenderPipelines.computeIfAbsent(Pair.of(colorAttachmentFormats, pipeline), _key -> {
            List<ColorTargetState> colorTargets = new ArrayList<>();
            for (var attachment : this.colorAttachments) {
                colorTargets.add(new ColorTargetState(
                    pipeline.getColorTargetState().blendFunction(),
                    attachment.textureView().texture().getFormat(),
                    pipeline.getColorTargetState().writeMask()
                ));
            }

            DepthStencilState depthState = null;
            var originalDepthState = pipeline.getDepthStencilState();
            if (originalDepthState != null) {
                depthState = new DepthStencilState(
                    originalDepthState.depthTest(),
                    originalDepthState.writeDepth(),
                    originalDepthState.depthBiasScaleFactor(),
                    originalDepthState.depthBiasConstant()
                );
            }

            return new RenderPipeline(
                pipeline.getLocation(),
                pipeline.getVertexShader(),
                pipeline.getFragmentShader(),
                pipeline.getShaderDefines(),
                pipeline.getBindGroupLayouts(),
                colorTargets.toArray(new ColorTargetState[0]),
                depthState,
                pipeline.getPolygonMode(),
                pipeline.isCull(),
                pipeline.getVertexFormatBindings(),
                pipeline.getPrimitiveTopology(),
                pipeline.getSortKey()
            ) {};
        });
    }

}
