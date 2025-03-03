package fewizz.canpipe;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.KHRDebug;

import com.mojang.blaze3d.systems.RenderSystem;

import fewizz.canpipe.mixin.GlStateManagerAccessor;

public class GFX {

    public static void glEnableCubemapSeamless() {
        GL33C.glEnable(GL33C.GL_TEXTURE_CUBE_MAP_SEAMLESS);
    }

    public static void glFramebufferTextureLayer(
        int target, int attachment, int texture, int level, int layer
    ) {
        GL33C.glFramebufferTextureLayer(target, attachment, texture, level, layer);
    }

    public static void glDrawBuffers(int[] bufs) {
        GL33C.glDrawBuffers(bufs);
    }

    public static void glObjectLabel(int identifier, int name, CharSequence label) {
        KHRDebug.glObjectLabel(identifier, name, label);
    }

    public static void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, ByteBuffer pixels) {
        GL33C.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, pixels);
    }

    /**
     * Same as {@link com.mojang.blaze3d.systems.RenderSystem#bindTexture RenderSystem.bindTexture},
     * but additionally supports targets other than {@link org.lwjgl.opengl.GL11#GL_TEXTURE_2D GL_TEXTURE_2D}
    */
    public static void glBindTexture(int target, int id) {
        RenderSystem.assertOnRenderThreadOrInit();
        var TEXTURES = GlStateManagerAccessor.canpipe_getTEXTURES();
        int active  = GlStateManagerAccessor.canpipe_getActiveTexture();
        int otherId = TEXTURES[active].binding;

        if (id != otherId) {
            TEXTURES[active].binding = id;
            GL33C.glBindTexture(target, id);
        }
    }

}
