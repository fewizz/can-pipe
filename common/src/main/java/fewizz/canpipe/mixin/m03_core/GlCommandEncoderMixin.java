package fewizz.canpipe.mixin.m03_core;

import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;

import fewizz.canpipe.Uniforms;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;

@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderMixin {

    @Shadow private boolean inRenderPass;

    @ModifyArg(
        method = "createRenderPass("+
            "Ljava/util/function/Supplier;"+
            "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
            "Ljava/util/OptionalInt;"+
            "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
            "Ljava/util/OptionalDouble;"+
        ")Lcom/mojang/blaze3d/systems/RenderPass;",
        at = @At(
            value="INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_glBindFramebuffer(II)V"
        ),
        index = 1
    )
    private int onCreateRenderPass(int framebufferID) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            /*int renderTarget = -1;
            if (framebufferID == p.solidFramebuffer.glID() || framebufferID == p.defaultFramebuffer.glID()) {
                renderTarget = 0;
            }
            else */

            int renderTarget = 0;

            if (framebufferID == p.translucentTerrainFramebuffer.glID()) {
                renderTarget = 1;
            }
            else if (framebufferID == p.translucentItemEntityFramebuffer.glID()) {
                renderTarget = 2;
            }
            else if (framebufferID == p.particlesFramebuffer.glID()) {
                renderTarget = 3;
            }
            if (renderTarget != Uniforms.CANPIPE_RENDER_TARGET.value) {
                Uniforms.CANPIPE_RENDER_TARGET.value = renderTarget;
                try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                    var builder = Std140Builder.onStack(memoryStack, Uniforms.MATERIAL_PROGRAM_UBO.size());
                    Uniforms.MATERIAL_PROGRAM.writeTo(builder);
                    this.inRenderPass = false;
                    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(Uniforms.MATERIAL_PROGRAM_UBO.slice(), builder.get());
                    this.inRenderPass = true;
                }
            }
        }

        return framebufferID;
    }

}
