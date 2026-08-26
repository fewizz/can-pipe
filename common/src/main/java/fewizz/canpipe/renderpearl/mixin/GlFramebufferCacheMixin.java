package fewizz.canpipe.renderpearl.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.backend.opengl.FrameBufferAttachment;
import com.mojang.renderpearl.backend.opengl.FrameBufferCache;

import fewizz.canpipe.renderpearl.GpuTextureViewExtended;

@Mixin(FrameBufferCache.class)
public class GlFramebufferCacheMixin {

    @WrapOperation(
        method = "createFbo",
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
