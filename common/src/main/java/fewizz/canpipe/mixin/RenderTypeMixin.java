package fewizz.canpipe.mixin;

import java.util.Map.Entry;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
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
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

@Mixin(RenderType.class)
public class RenderTypeMixin {

    @Shadow @Final private RenderSetup state;

    private RenderPipeline canpipe_getReplacedRenderPipeline(RenderPipeline renderPipeline) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            renderPipeline = p.replaceRenderPipeline(renderPipeline);
        }
        return renderPipeline;
    }

    @ModifyExpressionValue(
        method = {"format", "mode", "pipeline", "draw"},
        require = 4,
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderSetup;"+
                "pipeline:"+
                "Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
        )
    )
    RenderPipeline replaceRenderPipeline(RenderPipeline original) {
        return this.canpipe_getReplacedRenderPipeline(original);
    }

    @WrapOperation(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass("+
                "Ljava/util/function/Supplier;"+
                "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
                "Ljava/util/OptionalInt;"+
                "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
                "Ljava/util/OptionalDouble;"+
            ")Lcom/mojang/blaze3d/systems/RenderPass;"
        )
    )
    RenderPass onCreateRenderPass(
        CommandEncoder instance, Supplier<String> nameSupplier, GpuTextureView gpuTextureView, OptionalInt optionalInt, @Nullable GpuTextureView gpuTextureView2, OptionalDouble optionalDouble,
        Operation<RenderPass> operation,
        @Local RenderTarget renderTarget
    ) {
        if (renderTarget instanceof Framebuffer framebuffer) {
            return Pipelines.getCurrent().createRenderPass(instance, nameSupplier, framebuffer);
        }
        return operation.call(instance, nameSupplier, gpuTextureView, optionalInt, gpuTextureView2, optionalDouble);
    }

    @Inject(
        method = "draw",
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
        @Local Entry<String, RenderSetup.TextureAndSampler> entry,
        @Local RenderPass renderPass
    ) {
        if (entry.getKey().equals("Sampler0")) {
            Pipeline.bindSpritesExtentsSampler(renderPass, entry.getValue().textureView());
        }

    }

}
