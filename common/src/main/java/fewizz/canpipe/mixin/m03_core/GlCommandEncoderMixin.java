package fewizz.canpipe.mixin.m03_core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.mojang.blaze3d.opengl.GlCommandEncoder;

import fewizz.canpipe.pipeline.MaterialProgram;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;

@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderMixin {

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
            int renderTarget = -1;
            if (framebufferID == p.solidFramebuffer.glID() || framebufferID == p.defaultFramebuffer.glID()) {
                renderTarget = 0;
            }
            else if (framebufferID == p.translucentTerrainFramebuffer.glID()) {
                renderTarget = 1;
            }
            else if (framebufferID == p.translucentItemEntityFramebuffer.glID()) {
                renderTarget = 2;
            }
            else if (framebufferID == p.particlesFramebuffer.glID()) {
                renderTarget = 3;
            }
            MaterialProgram.CANPIPE_RENDER_TARGET.value = renderTarget;
        }

        return framebufferID;
    }

}
