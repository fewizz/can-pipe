package fewizz.canpipe.pipeline;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.KHRDebug;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import fewizz.canpipe.mixin.GlStateManagerAccessor;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;


public class Texture extends AbstractTexture {
    final String name;
    public final Vector3i extent;
    final int target;
    final int internalFormat;
    final int pixelFormat;
    final int pixelDataType;
    final int maxLod;
    final boolean isWidthWindowSizeDependent;
    final boolean isHeightWindowSizeDependent;

    public Texture(
        ResourceLocation pipelineLocation,
        String name,
        Vector3i extent,
        int target,
        int internalFormat,
        int pixelFormat,
        int pixeDataType,
        int maxLod,
        IntIntPair params[]
    ) {
        this.name = name;
        this.extent = extent;
        this.target = target;
        this.internalFormat = internalFormat;
        this.pixelFormat = pixelFormat;
        this.pixelDataType = pixeDataType;
        this.maxLod = maxLod;
        this.isWidthWindowSizeDependent = extent.x == 0;
        this.isHeightWindowSizeDependent = extent.y == 0;

        Minecraft mc = Minecraft.getInstance();
        if (isWidthWindowSizeDependent) {
            extent.x = mc.getWindow().getWidth();
        }
        if (isHeightWindowSizeDependent) {
            extent.y = mc.getWindow().getHeight();
        }

        bind();

        for (var p : params) {
            GlStateManager._texParameter(target, p.firstInt(), p.secondInt());
        }

        if (maxLod > 0) {
            GlStateManager._texParameter(target, GL33C.GL_TEXTURE_MIN_LOD, 0);
            GlStateManager._texParameter(target, GL33C.GL_TEXTURE_MAX_LOD, maxLod);
            GlStateManager._texParameter(target, GL33C.GL_TEXTURE_MAX_LEVEL, maxLod);
            GlStateManager._texParameter(target, GL33C.GL_TEXTURE_LOD_BIAS, 0.0F);
        }

        allocate();
        KHRDebug.glObjectLabel(GL33C.GL_TEXTURE, getId(), pipelineLocation.toString()+"-"+name);
    }

    private void allocate() {
        bind();

        for (int i = 0; i <= maxLod; ++i) {
            if (target == GL33C.GL_TEXTURE_3D) {
                GL33C.glTexImage3D(target, i, internalFormat, this.extent.x >> i, this.extent.y >> i, this.extent.z >> i, 0, pixelFormat, pixelDataType, (ByteBuffer) null);
            } else if (target == GL33C.GL_TEXTURE_2D_ARRAY) {
                GL33C.glTexImage3D(target, i, internalFormat, this.extent.x >> i, this.extent.y >> i, this.extent.z, 0, pixelFormat, pixelDataType, (ByteBuffer) null);
            } else if (target == GL33C.GL_TEXTURE_CUBE_MAP) {
                for (int face = 0; face < 6; ++face) {
                    GlStateManager._texImage2D(GL33C.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, i, internalFormat, this.extent.x >> i, this.extent.y >> i, 0, pixelFormat, pixelDataType, (IntBuffer) null);
                }
            } else {
                GlStateManager._texImage2D(target, i, internalFormat, this.extent.x >> i, this.extent.y >> i, 0, pixelFormat, pixelDataType, (IntBuffer) null);
            }
        }
    }

    public void onWindowSizeChanged(int w, int h) {
        if (isWidthWindowSizeDependent) {
            this.extent.x = w;
        }
        if (isHeightWindowSizeDependent) {
            this.extent.y = h;
        }
        if (isWidthWindowSizeDependent || isHeightWindowSizeDependent) {
            allocate();
        }
    }

    /**
     * Same as {@link com.mojang.blaze3d.systems.RenderSystem#bindTexture RenderSystem.bindTexture},
     * but additionally supports targets other than {@link org.lwjgl.opengl.GL11#GL_TEXTURE_2D GL_TEXTURE_2D}
    */
    public static void bind(int id, int target) {
        RenderSystem.assertOnRenderThreadOrInit();
        var TEXTURES = GlStateManagerAccessor.canpipe_getTEXTURES();
        int active  = GlStateManagerAccessor.canpipe_getActiveTexture();
        if (id != TEXTURES[active].binding) {
            TEXTURES[active].binding = id;
            GL33C.glBindTexture(target, id);
        }
    }

    public void bind() {
        bind(getId(), target);
    }

    @Override
    public void close() {
        this.releaseId();
    }

}
