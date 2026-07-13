package fewizz.canpipe.b3d.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.FrameBufferAttachment;
import com.mojang.blaze3d.opengl.FrameBufferCache;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.GpuTextureViewExtended;

@Mixin(FrameBufferCache.class)
public class GlFramebufferCacheMixin {

    @Overwrite
    int createFbo(
        final FrameBufferCache.CacheKey key,
		final DirectStateAccess dsa,
		final List<FrameBufferAttachment> colorAttachments,
		@Nullable final FrameBufferAttachment depthAttachment
    ) {
        int fbo = dsa.createFrameBufferObject();
		int colorAttachmentCount = colorAttachments.size();
		int[] colorIds = new int[colorAttachmentCount];
		int[] mipLevels = new int[colorAttachmentCount];

		for (int i = 0; i < colorAttachmentCount; i++) {
			FrameBufferAttachment attachment = (FrameBufferAttachment)colorAttachments.get(i);
			if (attachment != null) {
				colorIds[i] = attachment.glId();
				mipLevels[i] = canpipe_appendLayer(attachment, attachment.fboMipLevel());
				attachment.addAssociatedFbo(key);
			} else {
				colorIds[i] = 0;
				mipLevels[i] = 0;
			}
		}

		if (depthAttachment != null) {
			depthAttachment.addAssociatedFbo(key);
		}

		dsa.bindFrameBufferTextures(
			fbo, colorIds, mipLevels,
			depthAttachment == null ? 0 : depthAttachment.glId(),
			depthAttachment == null ? 0 : canpipe_appendLayer(depthAttachment, depthAttachment.fboMipLevel()),
			0
		);
		return fbo;
    }

    @Unique
    private static int canpipe_appendLayer(FrameBufferAttachment attachment, int mip) {
        if (attachment instanceof GpuTextureViewExtended tve && ((GpuTextureView) tve).texture().getDepthOrLayers() > 1) {
            mip |= (tve.canpipe_baseArrayLayer() << 16) | (1 << 31);
        }
        return mip;
    }

}
