package fewizz.canpipe.b3d.mixin;

import org.lwjgl.opengl.ARBDirectStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(targets = "com.mojang.blaze3d.opengl.DirectStateAccess$Core")
public class GlDirectStateAccessCoreMixin {

    @WrapOperation(
        method = "bindFrameBufferTextures",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/opengl/ARBDirectStateAccess;glNamedFramebufferTexture(IIII)V"
        )
    )
    void layeredAttachment(
        int framebuffer, int attachment, int texture, int level,
        Operation<Void> operation
    ) {
        int prevLevel = level;
        level = (level << 1) >>> 1;
        if (prevLevel != level) {
            int layer = level >> 16;
            level = level & 0xFFFF;
            ARBDirectStateAccess.glNamedFramebufferTextureLayer(framebuffer, attachment, texture, level, layer);
        }
        else {
            operation.call(framebuffer, attachment, texture, level);
        }
    }

}
