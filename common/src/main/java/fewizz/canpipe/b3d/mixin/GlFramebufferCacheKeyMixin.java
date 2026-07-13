package fewizz.canpipe.b3d.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.opengl.FrameBufferAttachment;
import com.mojang.blaze3d.opengl.FrameBufferCache;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.GpuTextureViewExtended;

@Mixin(FrameBufferCache.CacheKey.class)
public class GlFramebufferCacheKeyMixin {

    @Final private int[] data;

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "java.util.Arrays.hashCode([I)I"
        )
    )
    void takeLayerIntoAccount(CallbackInfo ci, @Local List<FrameBufferAttachment> colorAttachments, @Local FrameBufferAttachment depthAttachment) {
        for (int i = 0; i < colorAttachments.size(); i++) {
            FrameBufferAttachment attachment = (FrameBufferAttachment) colorAttachments.get(i);

            if (
                attachment != null && attachment instanceof GpuTextureViewExtended tve &&
                ((GpuTextureView) tve).texture().getDepthOrLayers() > 1  // Such texture is created as GL_TEXTURE_2D_ARRAY
            ) {
                this.data[i * 2 + 1] |= canpipe_layerData(attachment);
                // tve.canpipe_layerCount() *should* be 1
            }
        }

        if (depthAttachment != null) {
            this.data[colorAttachments.size() * 2 + 1] = canpipe_layerData(depthAttachment);
        }
    }

    @Unique
    private static int canpipe_layerData(FrameBufferAttachment attachment) {
        if (attachment instanceof GpuTextureViewExtended tve && ((GpuTextureView) tve).texture().getDepthOrLayers() > 1) {
            return (tve.canpipe_baseArrayLayer() << 16) | (1 << 31);
        }
        return 0;
    }

}
