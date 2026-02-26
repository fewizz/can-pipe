package fewizz.canpipe.mixin;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;

@Mixin(ParticleFeatureRenderer.class)
public class ParticleFeatureRendererMixin {

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
    RenderPass replaceColorAttachments(
        CommandEncoder instance, Supplier<String> nameSupplier, GpuTextureView colorTextureView, OptionalInt clearColor, @Nullable GpuTextureView depthTextureView, OptionalDouble clearDepth,
        Operation<RenderPass> operation,
        @Local(ordinal = 0) RenderTarget renderTargetMain
    ) {
        if (renderTargetMain instanceof Framebuffer framebuffer) {
            return Pipelines.getCurrent().createRenderPass(instance, nameSupplier, framebuffer);
        }
        return operation.call(instance, nameSupplier, colorTextureView, clearColor, depthTextureView, clearDepth);
    }

}
