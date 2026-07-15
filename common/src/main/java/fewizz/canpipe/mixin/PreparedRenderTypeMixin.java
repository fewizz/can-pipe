package fewizz.canpipe.mixin;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;

import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;

@Mixin(PreparedRenderType.class)
public class PreparedRenderTypeMixin {

    @Shadow @Final RenderPipeline pipeline;
    @Shadow @Final OutputTarget outputTarget;

    @ModifyExpressionValue(
        method = "drawFromBuffer("+
            "Lcom/mojang/blaze3d/buffers/GpuBuffer;"+
            "Lcom/mojang/blaze3d/buffers/GpuBuffer;"+
            "Lcom/mojang/blaze3d/IndexType;"+
            "III"+
        ")V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/OutputTarget;getRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"
        )
    )
    RenderTarget replaceRenderTarget(RenderTarget renderTarget) {
        Pipeline p = Pipelines.getCurrent();
        if (
            RenderSystem.outputColorTextureOverride == null &&
            RenderSystem.outputDepthTextureOverride == null &&
            p != null
        ) {
            renderTarget = p.replaceRenderTarget(renderTarget, this.pipeline, this.outputTarget);
        }
        return renderTarget;
    }

    @ModifyExpressionValue(
        method = "drawFromBuffer("+
            "Lcom/mojang/blaze3d/buffers/GpuBuffer;"+
            "Lcom/mojang/blaze3d/buffers/GpuBuffer;"+
            "Lcom/mojang/blaze3d/IndexType;"+
            "III"+
        ")V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/rendertype/PreparedRenderType;"+
                "pipeline:"+
                "Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
        )
    )
    RenderPipeline replaceRenderPipeline(RenderPipeline renderPipeline, @Local RenderTarget renderTarget) {
        if (
            renderTarget instanceof Framebuffer fb &&
            RenderSystem.outputColorTextureOverride == null &&
            RenderSystem.outputDepthTextureOverride == null
        ) {
            Pipeline p = Pipelines.getCurrent();
            renderPipeline = p.getReplacedRenderPipeline(renderPipeline, fb.getFormats());
        }
        return renderPipeline;
    }

    @WrapOperation(
        method = "drawFromBuffer("+
            "Lcom/mojang/blaze3d/buffers/GpuBuffer;"+
            "Lcom/mojang/blaze3d/buffers/GpuBuffer;"+
            "Lcom/mojang/blaze3d/IndexType;"+
            "III"+
        ")V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass("+
                "Ljava/util/function/Supplier;"+
                "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
                "Ljava/util/Optional;"+
                "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
                "Ljava/util/OptionalDouble;"+
            ")Lcom/mojang/blaze3d/systems/RenderPass;"
        )
    )
    RenderPass onCreateRenderPass(
        CommandEncoder instance, Supplier<String> nameSupplier,
        GpuTextureView colorTexture, Optional<Vector4f> clearColor,
        @Nullable GpuTextureView depthTexture, OptionalDouble clearDepth,
        Operation<RenderPass> operation,
        @Local RenderTarget renderTarget
    ) {
        if (
            renderTarget instanceof Framebuffer framebuffer &&
            RenderSystem.outputColorTextureOverride == null &&
            RenderSystem.outputDepthTextureOverride == null
        ) {
            return Pipelines.getCurrent().createRenderPass(instance, nameSupplier, framebuffer);
        }
        return operation.call(instance, nameSupplier, colorTexture, clearColor, depthTexture, clearDepth);
    }

    @Inject(
        method = "drawFromBuffer("+
            "Lcom/mojang/blaze3d/buffers/GpuBuffer;"+
            "Lcom/mojang/blaze3d/buffers/GpuBuffer;"+
            "Lcom/mojang/blaze3d/IndexType;"+
            "III"+
        ")V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPass;bindTexture("+
                "Ljava/lang/String;"+
                "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
                "Lcom/mojang/blaze3d/textures/GpuSampler;"+
            ")V"
        )
    )
    void bindSpritesExtentsBeforeRender(
        CallbackInfo ci,
        @Local PreparedRenderType.Texture texture,
        @Local RenderPass renderPass
    ) {
        if (texture.name().equals("Sampler0")) {
            Pipeline.bindSpritesExtentsSampler(renderPass, texture.textureView());
        }

    }

}
