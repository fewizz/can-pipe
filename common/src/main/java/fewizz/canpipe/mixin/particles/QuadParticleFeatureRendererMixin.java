package fewizz.canpipe.mixin.particles;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;

import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.MaterialProgramLoader;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;

@Mixin(QuadParticleFeatureRenderer.class)
public class QuadParticleFeatureRendererMixin {

    @Unique private static Framebuffer canpipe_selectedFramebuffer;

    // TODO
    /*@ModifyExpressionValue(
        method = "executeGroup",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;mainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    RenderTarget replaceMainRenderTargets(RenderTarget renderTarget) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) { renderTarget = p.shadowFramebufferOr(p.solidFramebuffer); }
        return renderTarget;
    }

    @ModifyExpressionValue(
        method = "executeGroup",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;particlesTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    RenderTarget replaceTranslucentRenderTargets(RenderTarget renderTarget) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) { renderTarget = p.shadowFramebufferOr(p.translucentParticlesFramebuffer); }
        return renderTarget;
    }

    @WrapOperation(
        method = "executeGroup",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass("+
                "Ljava/util/function/Supplier;"+
                "Lcom/mojang/renderpearl/api/textures/GpuTextureView;"+"Ljava/util/Optional;"+
                "Lcom/mojang/renderpearl/api/textures/GpuTextureView;"+"Ljava/util/OptionalDouble;"+
            ")Lcom/mojang/renderpearl/api/commands/RenderPass;",
            ordinal = 0
        )
    )
    RenderPass replaceRenderPass(
        CommandEncoder instance, Supplier<String> label,
        GpuTextureView colorTexture, Optional<Vector4f> clearColor,
        @Nullable GpuTextureView depthTexture, OptionalDouble clearDepth,
        Operation<RenderPass> operation,
        @Local(ordinal = 0) RenderTarget mainTarget,
        @Local(ordinal = 1) RenderTarget particleTarget,
        @Local(ordinal = 0, argsOnly = true) boolean translucent
    ) {
        RenderTarget renderTarget = translucent ? particleTarget : mainTarget;
        if (renderTarget instanceof Framebuffer framebuffer) {
            QuadParticleFeatureRendererMixin.canpipe_selectedFramebuffer = framebuffer;
            return Pipelines.getCurrent().createRenderPass(instance, label, framebuffer);
        }
        else {
            QuadParticleFeatureRendererMixin.canpipe_selectedFramebuffer = null;
            return operation.call(instance, label, colorTexture, clearColor, depthTexture, clearDepth);
        }
    }*/

    @ModifyExpressionValue(
        method = "lambda$prepareGroup$0",
        at = @At(
            value = "FIELD",
            target = "Lcom/mojang/blaze3d/vertex/DefaultVertexFormat;PARTICLE:Lcom/mojang/renderpearl/api/vertex/VertexFormat;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private static VertexFormat replaceVertexFormat(VertexFormat vertexFormat) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            vertexFormat = ((LevelRendererExtended) Minecraft.getInstance().levelRenderer).canpipe_getCurrentShadowCascadeIdx() >= 0
                ? CanPipe.VertexFormats.PARTICLE_SHADOW
                : CanPipe.VertexFormats.PARTICLE;
        }
        return vertexFormat;
    }

    // TODO
    @WrapOperation(
        method = "drawLayers",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getCompiledPipeline")
    )
    private static CompiledRenderPipeline replaceRenderPipeline(
        RenderPipeline pipeline,
        Operation<CompiledRenderPipeline> operation
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null &&
            pipeline == RenderPipelines.OPAQUE_PARTICLE ||
            pipeline == RenderPipelines.TRANSLUCENT_PARTICLE
        ) {
            MaterialProgramLoader loader = p.getMaterialProgramLoader(pipeline);
            if (loader != null) {
                Framebuffer fb = pipeline == RenderPipelines.OPAQUE_PARTICLE ? p.solidFramebuffer : p.translucentTerrainFramebuffer;  // TODO p.translucentParticlesFramebuffer;
                return loader.getOrCreateCompiledRenderPipeline(fb.getFormats());
            }
        }
        return operation.call(pipeline);
    }

    @Inject(
        method = "drawLayers",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/renderpearl/api/commands/RenderPass;setUniform("+
                "Ljava/lang/String;"+
                "Lcom/mojang/renderpearl/api/textures/GpuTextureView;"+
                "Lcom/mojang/renderpearl/api/textures/GpuSampler;"+
            ")V"
        )
    )
    private static void bindSpritesExtentsBeforeRender(
        CallbackInfo ci,
        @Local AbstractTexture texture,
        @Local RenderPass renderPass
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            Pipeline.bindSpritesExtentsSampler(renderPass, texture.getTextureView());
            p.bindUniforms(renderPass, p.solidFramebuffer, true);
            // renderPass.setUniform("", null);
        }
    }

}
