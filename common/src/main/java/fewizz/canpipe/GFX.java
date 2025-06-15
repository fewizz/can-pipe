package fewizz.canpipe;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL33C;

public class GFX {

    public static void glBindTexture(int target, int texture) {
        GL11C.glBindTexture(target, texture);
    }

    public static void glFramebufferTextureLayer(int target, int attachment, int texture, int level, int layer) {
        GL33C.glFramebufferTextureLayer(target, attachment, texture, level, layer);
    }

    public static void glDrawBuffers(int[] bufs) {
        GL33C.glDrawBuffers(bufs);
    }

    public static void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, ByteBuffer pixels) {
        GL33C.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, pixels);
    }

    public static void glClearDepth(double depth) {
        GL11C.glClearDepth(depth);
    }

    public static void glClearColor(float red, float green, float blue, float alpha) {
        GL11C.glClearColor(red, green, blue, alpha);
    }

    public static String glGetActiveUniform(int program, int index, IntBuffer size, IntBuffer type) {
        return GL20C.glGetActiveUniform(program, index, size, type);
    }

}
