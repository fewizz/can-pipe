package fewizz.canpipe.mixin;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.Uniforms;
import fewizz.canpipe.b3d.CommandEncoderExtended;
import fewizz.canpipe.pipeline.Framebuffer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;

@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin {

    @Inject(
        method = "renderGroup",
        at = @At("HEAD")
    )
    void preRenderGroup(CallbackInfo ci) {
        if (Uniforms.CANPIPE_ORIGIN_TYPE.get() == 1) { return; }
        Uniforms.CANPIPE_ORIGIN_TYPE.set(1);  // region

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            var builder = Std140Builder.onStack(memoryStack, Uniforms.MATERIAL_PROGRAM_UBO.size());
            Uniforms.MATERIAL_PROGRAM.writeTo(builder);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(Uniforms.MATERIAL_PROGRAM_UBO.slice(), builder.get());
        }
    }

    @Inject(
        method = "renderGroup",
        at = @At("RETURN")
    )
    void postRenderGroup(CallbackInfo ci) {
        if (Uniforms.CANPIPE_ORIGIN_TYPE.get() == 0) { return; }
        Uniforms.CANPIPE_ORIGIN_TYPE.set(0);  // camera

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            var builder = Std140Builder.onStack(memoryStack, Uniforms.MATERIAL_PROGRAM_UBO.size());
            Uniforms.MATERIAL_PROGRAM.writeTo(builder);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(Uniforms.MATERIAL_PROGRAM_UBO.slice(), builder.get());
        }
    }

    @Redirect(
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
        CommandEncoder instance, Supplier<String> supplier, GpuTextureView gpuTextureView, OptionalInt optionalInt, @Nullable GpuTextureView gpuTextureView2, OptionalDouble optionalDouble,
        @Local RenderTarget renderTarget
    ) {
        if (renderTarget instanceof Framebuffer framebuffer) {
            return ((CommandEncoderExtended) instance).canpipe_createRenderPass(supplier, framebuffer.colorAttachments, gpuTextureView2);
        }
        return instance.createRenderPass(supplier, gpuTextureView, optionalInt, gpuTextureView2, optionalDouble);
    }

}
