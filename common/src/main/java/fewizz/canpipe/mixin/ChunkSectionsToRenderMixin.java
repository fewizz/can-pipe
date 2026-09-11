package fewizz.canpipe.mixin;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.oit.OitRenderPassProvider;
import net.minecraft.client.renderer.oit.OitStage;

@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin {

    // TODO
    /*@WrapOperation(
        method = "renderOit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/oit/OitRenderPassProvider;createRenderPass("+
                "Lnet/minecraft/client/renderer/oit/OitStage;"+
                "Ljava/util/function/Supplier;"+
                "Lnet/minecraft/client/renderer/oit/OitRenderPassProvider$Parameters;"+
            ")Lcom/mojang/renderpearl/api/commands/RenderPass;"
        )
    )
    RenderPass replaceColorAttachments(
        OitStage stage, Supplier<String> label, OitRenderPassProvider.Parameters params,
        Operation<RenderPass> operation,
        @Local RenderTarget renderTarget
    ) {
        if (renderTarget instanceof Framebuffer framebuffer) {
            return Pipelines.getCurrent().createRenderPass(
                RenderSystem.getDevice().createCommandEncoder(), nameSupplier,
                framebuffer
            );
        }
        return operation.call(instance, nameSupplier, colorTexture, clearColor, depthTexture, clearDepth);
    }

    @Inject(
        method = "renderLayers",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;render("+
                "Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;"+
                "Lcom/mojang/renderpearl/api/commands/RenderPass;"+
                "Lcom/mojang/renderpearl/api/buffers/GpuBuffer;"+
                "Lcom/mojang/renderpearl/api/pipeline/IndexType;"+
                "Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"+
                "Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"+
            ")V"
        )
    )
    void bindSpritesExtentsBeforeDrawing(CallbackInfo ci, @Local RenderPass renderPass, @Local ChunkSectionLayer chunkSectionLayer, @Local(name="atlas") GpuTextureView atlas) {
        Pipeline.bindSpritesExtentsSampler(renderPass, atlas);
    }*/

}
