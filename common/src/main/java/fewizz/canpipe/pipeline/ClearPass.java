package fewizz.canpipe.pipeline;

import com.mojang.renderpearl.api.commands.CommandEncoder;

import fewizz.canpipe.renderpearl.CommandEncoderExtended;
import fewizz.canpipe.renderpearl.GpuTextureViewExtended;
import net.minecraft.resources.Identifier;

public class ClearPass extends Pass {
    final Framebuffer framebuffer;

    public ClearPass(Identifier id, Framebuffer framebuffer) {
        super(id);
        this.framebuffer = framebuffer;
    }

    @Override
    public void apply(CommandEncoder commandEncoder) {
        for (int i = 0; i < this.framebuffer.colorTextureViews.length; ++i) {
            var attachment = this.framebuffer.colorAttachments.get(i);
            ((CommandEncoderExtended) commandEncoder).canpipe_clearColorTexture(
                attachment.texture().getTexture(),
                attachment.clearColor(),
                this.framebuffer.colorTextureViews[i].baseMipLevel(),
                this.framebuffer.colorTextureViews[i].mipLevels(),
                ((GpuTextureViewExtended) this.framebuffer.colorTextureViews[i]).canpipe_baseArrayLayer(),
                ((GpuTextureViewExtended) this.framebuffer.colorTextureViews[i]).canpipe_layerCount()
            );
        }
        if (this.framebuffer.getDepthTexture() != null) {
            ((CommandEncoderExtended) commandEncoder).canpipe_clearDepthTexture(
                this.framebuffer.depthAttachment.texture().getTexture(),
                this.framebuffer.depthAttachment.clearDepth(),
                this.framebuffer.getDepthTextureView().baseMipLevel(),
                this.framebuffer.getDepthTextureView().mipLevels(),
                ((GpuTextureViewExtended) this.framebuffer.getDepthTextureView()).canpipe_baseArrayLayer(),
                ((GpuTextureViewExtended) this.framebuffer.getDepthTextureView()).canpipe_layerCount()
            );
        }
    }

    @Override
    public void close() {}

}
