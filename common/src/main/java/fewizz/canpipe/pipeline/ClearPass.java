package fewizz.canpipe.pipeline;

import com.mojang.blaze3d.systems.CommandEncoder;

import fewizz.canpipe.b3d.CommandEncoderExtended;
import fewizz.canpipe.b3d.GpuTextureViewExtended;

class ClearPass extends Pass {
    final Framebuffer framebuffer;

    ClearPass(String name, Framebuffer framebuffer) {
        super(name);
        this.framebuffer = framebuffer;
    }

    @Override
    public void apply(CommandEncoder commandEncoder) {
        for (int i = 0; i < this.framebuffer.colorTextureViews.length; ++i) {
            ((CommandEncoderExtended) commandEncoder).canpipe_clearColorTexture(
                this.framebuffer.colorTextures[i],
                this.framebuffer.colorTextureClearColors[i],
                this.framebuffer.colorTextureViews[i].baseMipLevel(),
                this.framebuffer.colorTextureViews[i].mipLevels(),
                ((GpuTextureViewExtended) this.framebuffer.colorTextureViews[i]).canpipe_baseArrayLayer(),
                ((GpuTextureViewExtended) this.framebuffer.colorTextureViews[i]).canpipe_layerCount()
            );
        }
        if (this.framebuffer.getDepthTexture() != null) {
            ((CommandEncoderExtended) commandEncoder).canpipe_clearDepthTexture(
                this.framebuffer.getDepthTexture(),
                this.framebuffer.depthTextureClearDepth,
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
