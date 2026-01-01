package fewizz.canpipe.mixin;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
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

import fewizz.canpipe.b3d.CommandEncoderExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;

@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin {

    @Inject(
        method = "renderGroup",
        at = @At("HEAD")
    )
    void preRenderGroup(CallbackInfo ci) {
        LevelRendererExtended lre = (LevelRendererExtended) Minecraft.getInstance().levelRenderer;
        lre.canpipe_setOriginType(1);  // region
    }

    @Inject(
        method = "renderGroup",
        at = @At("RETURN")
    )
    void postRenderGroup(CallbackInfo ci) {
        LevelRendererExtended lre = (LevelRendererExtended) Minecraft.getInstance().levelRenderer;
        lre.canpipe_setOriginType(0);  // camera
    }

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
        CommandEncoder instance, Supplier<String> nameSupplier, GpuTextureView colorTextureView, OptionalInt clearColor, @Nullable GpuTextureView depthTextureView, OptionalDouble clearDepth,
        Operation<RenderPass> operation,
        @Local RenderTarget renderTarget,
        @Local ChunkSectionLayerGroup group
    ) {
        if (renderTarget instanceof Framebuffer framebuffer) {
            return Pipelines.getCurrent().createRenderPass((CommandEncoderExtended) instance, nameSupplier, framebuffer);
        }
        return operation.call(instance, nameSupplier, colorTextureView, clearColor, depthTextureView, clearDepth);
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
        // TODO Pipeline.bindSpritesExtentsSampler(renderPass, chunkSectionLayer.textureView());
    }

}
