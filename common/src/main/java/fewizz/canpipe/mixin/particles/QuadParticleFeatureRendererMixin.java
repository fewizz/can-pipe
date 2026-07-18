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
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;

@Mixin(QuadParticleFeatureRenderer.class)
public class QuadParticleFeatureRendererMixin {

    @Unique private static Framebuffer canpipe_selectedFramebuffer;

    @ModifyExpressionValue(
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
                "Lcom/mojang/blaze3d/textures/GpuTextureView;"+"Ljava/util/Optional;"+
                "Lcom/mojang/blaze3d/textures/GpuTextureView;"+"Ljava/util/OptionalDouble;"+
            ")Lcom/mojang/blaze3d/systems/RenderPass;",
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
    }

    @ModifyExpressionValue(
        method = "lambda$prepareGroup$0",
        at = @At(
            value = "FIELD",
            target = "Lcom/mojang/blaze3d/vertex/DefaultVertexFormat;PARTICLE:Lcom/mojang/blaze3d/vertex/VertexFormat;",
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

    @ModifyArg(
        method = "drawLayers",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"
        ),
        index = 0
    )
    private static RenderPipeline replaceRenderPipeline(RenderPipeline renderPipeline) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null && QuadParticleFeatureRendererMixin.canpipe_selectedFramebuffer != null) {
            renderPipeline = p.getReplacedRenderPipeline(renderPipeline, canpipe_selectedFramebuffer.getFormats());
        }
        return renderPipeline;
    }

    @Inject(
        method = "drawLayers",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPass;bindTexture("+
                "Ljava/lang/String;"+
                "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
                "Lcom/mojang/blaze3d/textures/GpuSampler;"+
            ")V"
        )
    )
    private static void bindSpritesExtentsBeforeRender(
        CallbackInfo ci,
        @Local AbstractTexture texture,
        @Local RenderPass renderPass
    ) {
        Pipeline.bindSpritesExtentsSampler(renderPass, texture.getTextureView());
    }

}
