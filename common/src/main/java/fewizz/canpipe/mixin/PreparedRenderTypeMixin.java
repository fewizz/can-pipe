package fewizz.canpipe.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
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
import com.mojang.blaze3d.GpuFormat;
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

    @Shadow @Final private RenderPipeline pipeline;
    @Shadow @Final private OutputTarget outputTarget;

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
        if (p != null) {
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
        if (renderTarget instanceof Framebuffer fb) {
            Pipeline p = Pipelines.getCurrent();
            Pair<List<GpuFormat>, @Nullable GpuFormat> formats;

            if (RenderSystem.outputColorTextureOverride != null) {
                List<GpuFormat> colorFormats = new ArrayList<>();
                if (fb.colorTextureViews.length > 0) {
                    colorFormats.add(RenderSystem.outputColorTextureOverride.texture().getFormat());
                }
                for (int i = 1; i < fb.colorTextureViews.length; ++i) {
                    colorFormats.add(null);
                }

                formats = Pair.of(colorFormats, RenderSystem.outputDepthTextureOverride.texture().getFormat());
            }
            else {
                formats = fb.getFormats();
            }

            renderPipeline = p.getReplacedRenderPipeline(renderPipeline, formats);
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
        CommandEncoder instance, Supplier<String> label,
        GpuTextureView colorTexture, Optional<Vector4f> clearColor,
        @Nullable GpuTextureView depthTexture, OptionalDouble clearDepth,
        Operation<RenderPass> operation,
        @Local RenderTarget renderTarget
    ) {
        if (renderTarget instanceof Framebuffer framebuffer) {
            Pipeline p = Pipelines.getCurrent();
            return p.createRenderPass(instance, label, framebuffer);
        }
        return operation.call(instance, label, colorTexture, clearColor, depthTexture, clearDepth);
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
