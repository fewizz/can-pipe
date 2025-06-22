package fewizz.canpipe.mixin.m03_core;

import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import fewizz.canpipe.Uniforms;
import fewizz.canpipe.mixininterface.CommandEncoderExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;

@Mixin(GlCommandEncoder.class)
public abstract class GlCommandEncoderMixin implements CommandEncoderExtended {

    @Shadow private boolean inRenderPass;
    @Unique private VertexFormatElement.Type canpipe_type = null;

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
            if (renderTarget != Uniforms.CANPIPE_RENDER_TARGET.get()) {
                Uniforms.CANPIPE_RENDER_TARGET.set(renderTarget);
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

    @ModifyExpressionValue(
        method = "writeToTexture("+
            "Lcom/mojang/blaze3d/textures/GpuTexture;"+
            "Ljava/nio/IntBuffer;"+
            "Lcom/mojang/blaze3d/platform/NativeImage$Format;"+
            "IIIIII"+
        ")V",
        at = @At(
            value = "CONSTANT",
            args = "intValue=5121"  // UNSIGNED_BYTE
        )
    )
    public int writeToTexture(int type) {
        if (this.canpipe_type != null) {
            type = GlConst.toGl(this.canpipe_type);
        }
        return type;
    }

    @Override
    public void canpipe_writeToTexture(
        GpuTexture gpuTexture, IntBuffer intBuffer, Format format, int i, int j, int k, int l, int m, int n,
        VertexFormatElement.Type type  // added
    ) {
        try {
            this.canpipe_type = type;
            // buffer size check will be incorrect, but anyway...
            this.writeToTexture(gpuTexture, intBuffer, format, i, j, k, l, m, n);
        } finally {
            this.canpipe_type = null;
        }
    }

}
