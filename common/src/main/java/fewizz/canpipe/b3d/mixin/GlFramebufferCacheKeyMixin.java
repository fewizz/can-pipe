package fewizz.canpipe.b3d.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.FrameBufferAttachment;
import com.mojang.blaze3d.opengl.FrameBufferCache;

import fewizz.canpipe.b3d.GpuTextureViewExtended;

@Mixin(FrameBufferCache.CacheKey.class)
public class GlFramebufferCacheKeyMixin {

    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/FrameBufferAttachment;fboMipLevel()I"
        )
    )
    int takeLayerIntoAccount(
        FrameBufferAttachment attachment,
        Operation<Integer> operation
    ) {
        int layer = 0;
        if (attachment instanceof GpuTextureViewExtended tve) {
            layer = tve.canpipe_baseArrayLayer();
        }
        return operation.call(attachment) | (layer << 16);
    }

}
