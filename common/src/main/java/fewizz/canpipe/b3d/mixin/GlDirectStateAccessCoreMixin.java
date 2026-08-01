package fewizz.canpipe.b3d.mixin;

import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.GL33C;
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
        int realTextureTarget = GlStateManagerAccessor.canpipe_getTextureTarget(texture);
        if (realTextureTarget == GL33C.GL_TEXTURE_2D_ARRAY || realTextureTarget == GL33C.GL_TEXTURE_CUBE_MAP) {
            int layer = level >>> 16;
            level = level & 0xFFFF;
            ARBDirectStateAccess.glNamedFramebufferTextureLayer(framebuffer, attachment, texture, level, layer);
        }
        else {
            operation.call(framebuffer, attachment, texture, level);
        }
    }

}
