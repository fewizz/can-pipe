package fewizz.canpipe.mixin;

import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.GFX;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.MaterialProgram;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.CompiledShaderProgram;

@Mixin(VertexBuffer.class)
public abstract class VertexBufferMixin {

    @Unique
    private boolean trap = true;

    @Shadow
    abstract void draw();

    @Inject(
        method = "draw",
        at = @At("HEAD"),
        cancellable = true
    )
    void rightBeforeDrawing(CallbackInfo ci) {
        if (this.trap && RenderSystem.getShader() instanceof MaterialProgram mp && mp.vertexFormat != CanPipe.VertexFormats.BLOCK && mp.depth) {
            this.trap = false;

            Pipeline p = Pipelines.getCurrent();
            Framebuffer shadowFramebuffer = p.framebuffers.get(p.skyShadows.framebufferName());
            // shadowFramebuffer.bindWrite(false); it should already be bound (:pray:)

            for (int cascade = 0; cascade < 4; ++cascade) {
                GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, shadowFramebuffer.depthAttachment.texture().getId(), 0, cascade);
                mp.FRXU_CASCADE.set(cascade);
                mp.FRXU_CASCADE.upload();
                this.draw();
            }

            this.trap = true;
            ci.cancel();
        }
    }

}
