package fewizz.canpipe.mixin;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;

@Mixin(ParticleFeatureRenderer.class)
public class ParticleFeatureRendererMixin {

    @ModifyExpressionValue(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"
        )
    )
    RenderTarget replaceMainRenderTargets(RenderTarget renderTarget) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            renderTarget = p.shadowFramebufferOr(p.solidFramebuffer);
        }
        return renderTarget;
    }

    @ModifyExpressionValue(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;getParticlesTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"
        )
    )
    RenderTarget replaceTranslucentRenderTargets(RenderTarget renderTarget) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            renderTarget = p.shadowFramebufferOr(p.translucentParticlesFramebuffer);
        }
        return renderTarget;
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass("+
                "Ljava/util/function/Supplier;"+
                "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
                "Ljava/util/OptionalInt;"+
                "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
                "Ljava/util/OptionalDouble;"+
            ")Lcom/mojang/blaze3d/systems/RenderPass;",
            ordinal = 0
        )
    )
    RenderPass replaceRenderPass(
        CommandEncoder instance, Supplier<String> label,
        GpuTextureView colorTexture, OptionalInt clearColor,
        @Nullable GpuTextureView depthTexture, OptionalDouble clearDepth,
        Operation<RenderPass> operation,
        @Local(ordinal = 0) RenderTarget mainTarget,
        @Local(ordinal = 1) RenderTarget particleTarget,
        @Local(ordinal = 0, argsOnly = true) boolean translucent
    ) {
        RenderTarget renderTarget = translucent ? particleTarget : mainTarget;
        if (renderTarget instanceof Framebuffer framebuffer) {
            return Pipelines.getCurrent().createRenderPass(instance, label, framebuffer);
        }
        return operation.call(instance, label, colorTexture, clearColor, depthTexture, clearDepth);
    }

}
