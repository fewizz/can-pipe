package fewizz.canpipe.mixin;

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
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;

@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin {

    @Shadow @Final private GpuTextureView textureView;

    @WrapOperation(
        method = "renderGroup",
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
    RenderPass replaceColorAttachments(
        CommandEncoder instance, Supplier<String> nameSupplier,
        GpuTextureView colorTexture, OptionalInt clearColor,
        @Nullable GpuTextureView depthTexture, OptionalDouble clearDepth,
        Operation<RenderPass> operation,
        @Local RenderTarget renderTarget
    ) {
        if (renderTarget instanceof Framebuffer framebuffer) {
            return Pipelines.getCurrent().createRenderPass(instance, nameSupplier, framebuffer);
        }
        return operation.call(instance, nameSupplier, colorTexture, clearColor, depthTexture, clearDepth);
    }

    @Inject(
        method = "renderGroup",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPass;drawMultipleIndexed("+
                "Ljava/util/Collection;"+
                "Lcom/mojang/blaze3d/buffers/GpuBuffer;"+
                "Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;"+
                "Ljava/util/Collection;"+
                "Ljava/lang/Object;"+
            ")V"
        )
    )
    void bindSpritesExtentsBeforeDrawing(CallbackInfo ci, @Local RenderPass renderPass, @Local ChunkSectionLayer chunkSectionLayer) {
        Pipeline.bindSpritesExtentsSampler(renderPass, this.textureView);
    }

}
