package fewizz.canpipe.mixin.m03_core;

import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;

import fewizz.canpipe.Uniforms;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;

@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin {

    @Inject(
        method = "renderGroup",
        at = @At("HEAD")
    )
    void preRenderGroup(CallbackInfo ci) {
        if (Uniforms.CANPIPE_ORIGIN_TYPE.value == 1) { return; }
        Uniforms.CANPIPE_ORIGIN_TYPE.value = 1; // region

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
        if (Uniforms.CANPIPE_ORIGIN_TYPE.value == 0) { return; }
        Uniforms.CANPIPE_ORIGIN_TYPE.value = 0; // camera

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            var builder = Std140Builder.onStack(memoryStack, Uniforms.MATERIAL_PROGRAM_UBO.size());
            Uniforms.MATERIAL_PROGRAM.writeTo(builder);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(Uniforms.MATERIAL_PROGRAM_UBO.slice(), builder.get());
        }
    }

}
