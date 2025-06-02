package fewizz.canpipe.mixin.m04_core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.mixininterface.CompositeRenderTypeExtended;
import fewizz.canpipe.mixininterface.CompositeStateExtended;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

@Mixin(RenderType.class)
public class RenderTypeMixin {

    @WrapOperation(
        method = "create("+
            "Ljava/lang/String;"+
            "IZZ"+
            "Lcom/mojang/blaze3d/pipeline/RenderPipeline;"+
            "Lnet/minecraft/client/renderer/RenderType$CompositeState;"+
        ")Lnet/minecraft/client/renderer/RenderType$CompositeRenderType;",
        at = @At(
            value = "NEW",
            target = "("+
                "Ljava/lang/String;"+
                "IZZ"+
                "Lcom/mojang/blaze3d/pipeline/RenderPipeline;"+
                "Lnet/minecraft/client/renderer/RenderType$CompositeState;"+
            ")Lnet/minecraft/client/renderer/RenderType$CompositeRenderType;"
        )
    )
    private static RenderType.CompositeRenderType onCreate(
        String name,
        int bufferSize,
        boolean affectsCrumbling,
        boolean sortOnUpload,
        RenderPipeline renderPipeline,
        RenderType.CompositeState compositeState,
        Operation<RenderType.CompositeRenderType> operation
    ) {
        RenderType.CompositeRenderType renderType = null;

        if (
            renderPipeline == RenderPipelines.SOLID ||
            renderPipeline == RenderPipelines.CUTOUT ||
            renderPipeline == RenderPipelines.CUTOUT_MIPPED ||
            renderPipeline == RenderPipelines.TRANSLUCENT ||
            renderPipeline == RenderPipelines.TRANSLUCENT_MOVING_BLOCK ||
            renderPipeline == RenderPipelines.ENTITY_SOLID ||
            renderPipeline == RenderPipelines.ENTITY_CUTOUT ||
            renderPipeline == RenderPipelines.ENTITY_CUTOUT_NO_CULL ||
            renderPipeline == RenderPipelines.ENTITY_CUTOUT_NO_CULL_Z_OFFSET ||
            renderPipeline == RenderPipelines.ENTITY_TRANSLUCENT ||
            renderPipeline == RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE ||
            renderPipeline == RenderPipelines.ENTITY_NO_OUTLINE ||
            renderPipeline == RenderPipelines.ARMOR_CUTOUT_NO_CULL ||
            renderPipeline == RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL ||
            renderPipeline == RenderPipelines.ARMOR_TRANSLUCENT ||
            renderPipeline == RenderPipelines.ITEM_ENTITY_TRANSLUCENT_CULL ||
            renderPipeline == RenderPipelines.EYES ||
            renderPipeline == RenderPipelines.OPAQUE_PARTICLE ||
            renderPipeline == RenderPipelines.TRANSLUCENT_PARTICLE
        ) {
            var cse = (CompositeStateExtended)(Object) compositeState;
            var compositeStateBuilder = cse.canpipe_builderFromCurrentState();
            var originalOutputState = cse.canpipe_getOutputState();

            renderType = operation.call(
                name, bufferSize, affectsCrumbling, sortOnUpload, renderPipeline,
                compositeStateBuilder.createCompositeState(cse.canpipe_getOutlineProperty())
            );

            VertexFormat format;

            if (renderPipeline.getVertexFormat() == DefaultVertexFormat.BLOCK) {
                format = CanPipe.VertexFormats.BLOCK;
            }
            else if (renderPipeline.getVertexFormat() == DefaultVertexFormat.NEW_ENTITY) {
                format = CanPipe.VertexFormats.NEW_ENTITY;
            }
            else if (renderPipeline.getVertexFormat() == DefaultVertexFormat.PARTICLE) {
                format = CanPipe.VertexFormats.PARTICLE;
            }
            else {
                throw new RuntimeException(renderPipeline.getVertexFormat().toString());
            }

            float alphaCutout;
            if (
                originalOutputState == RenderStateShard.ITEM_ENTITY_TARGET ||
                renderPipeline.getVertexFormat() == DefaultVertexFormat.PARTICLE ||
                renderPipeline == RenderPipelines.CUTOUT ||
                renderPipeline == RenderPipelines.ENTITY_CUTOUT ||
                renderPipeline == RenderPipelines.ENTITY_CUTOUT_NO_CULL ||
                renderPipeline == RenderPipelines.ENTITY_CUTOUT_NO_CULL_Z_OFFSET ||
                renderPipeline == RenderPipelines.ENTITY_TRANSLUCENT ||
                renderPipeline == RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE ||
                renderPipeline == RenderPipelines.ARMOR_CUTOUT_NO_CULL ||
                renderPipeline == RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL ||
                renderPipeline == RenderPipelines.ARMOR_TRANSLUCENT
            ) {
                alphaCutout = 0.1F;
            }
            else if (renderPipeline == RenderPipelines.CUTOUT_MIPPED) {
                alphaCutout = 0.5F;
            }
            else {
                alphaCutout = 0.0F;
            }

            ((CompositeRenderTypeExtended) (Object) renderType).canpipe_setMaterialRenderPipelineCreationFunction(p -> {
                var pipeline = RenderPipeline.builder()
                    .withLocation(ResourceLocation.fromNamespaceAndPath("canpipe", "material"))
                    .withVertexShader(ResourceLocation.fromNamespaceAndPath("canpipe", "material"))
                    .withFragmentShader(ResourceLocation.fromNamespaceAndPath("canpipe", "material"))
                    .withDepthTestFunction(renderPipeline.getDepthTestFunction())
                    .withDepthBias(renderPipeline.getDepthBiasScaleFactor(), renderPipeline.getDepthBiasConstant())
                    .withPolygonMode(renderPipeline.getPolygonMode())
                    .withCull(renderPipeline.isCull())
                    .withColorWrite(renderPipeline.isWriteColor(), renderPipeline.isWriteAlpha())
                    .withDepthWrite(renderPipeline.isWriteDepth())
                    .withVertexFormat(format, renderPipeline.getVertexFormatMode())
                    .withShaderDefine("CANPIPE_ALPHA_CUTOUT", alphaCutout);

                if (renderPipeline.getBlendFunction().isPresent()) {
                    pipeline.withBlend(renderPipeline.getBlendFunction().get());
                }

                // MaterialProgram program = p.materialPrograms.get(format);
                // for (String sampler : program.getSamplers()) { pipeline.withSampler(sampler); }
                // for (var u : program.getUniforms()) { pipeline.withUniform(u.getName(), u.getType()); }

                return pipeline.build();
            });
            ((CompositeRenderTypeExtended) (Object) renderType).canpipe_setMaterialShadowRenderPipelineCreationFunction(p -> {
                var pipeline = RenderPipeline.builder()
                    .withLocation(ResourceLocation.fromNamespaceAndPath("canpipe", "material-shadow"))
                    .withVertexShader(ResourceLocation.fromNamespaceAndPath("canpipe", "material-shadow"))
                    .withFragmentShader(ResourceLocation.fromNamespaceAndPath("canpipe", "material-shadow"))
                    .withDepthTestFunction(renderPipeline.getDepthTestFunction())
                    .withDepthBias(p.shadows.offsetSlopeFactor(), p.shadows.offsetBiasUnits())
                    .withPolygonMode(renderPipeline.getPolygonMode())
                    .withCull(false)  // Light can pass through chunk edge. Not ideal solution
                    .withColorWrite(renderPipeline.isWriteColor(), renderPipeline.isWriteAlpha())
                    .withDepthWrite(renderPipeline.isWriteDepth())
                    .withVertexFormat(format, renderPipeline.getVertexFormatMode())
                    .withShaderDefine("CANPIPE_ALPHA_CUTOUT", alphaCutout);

                if (renderPipeline.getBlendFunction().isPresent()) {
                    pipeline.withBlend(renderPipeline.getBlendFunction().get());
                }

                // MaterialProgram program = p.shadows.materialPrograms().get(format);
                // for (String sampler : program.getSamplers()) { pipeline.withSampler(sampler); }
                // for (var u : program.getUniforms()) { pipeline.withUniform(u.getName(), u.getType()); }

                return pipeline.build();
            });
        }
        else {
            renderType = operation.call(
                name, bufferSize, affectsCrumbling, sortOnUpload,
                renderPipeline, compositeState
            );
        }

        return renderType;
    }

}
