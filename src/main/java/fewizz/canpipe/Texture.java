package fewizz.canpipe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL33C;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import fewizz.canpipe.mixin.GLStateManagerAccessor;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;


public class Texture extends AbstractTexture {
    final String name;
    final Vector3i extent;
    final int target;
    final int internalFormat;
    final int pixelFormat;
    final int pixelDataType;
    final int maxLod;
    final boolean widthWindowSizeDependent;
    final boolean heightWindowSizeDependent;

    public Texture(
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
        this.widthWindowSizeDependent = extent.x == 0;
        this.heightWindowSizeDependent = extent.y == 0;

        Minecraft mc = Minecraft.getInstance();
        if (widthWindowSizeDependent) {
            extent.x = mc.getWindow().getWidth();
        }
        if (heightWindowSizeDependent) {
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
        if (widthWindowSizeDependent) {
            this.extent.x = w;
        }
        if (heightWindowSizeDependent) {
            this.extent.y = h;
        }
        if (widthWindowSizeDependent || heightWindowSizeDependent) {
            allocate();
        }
    }

    public void bind() {
        RenderSystem.assertOnRenderThreadOrInit();
        int id = this.getId();
        var TEXTURES = GLStateManagerAccessor.canpipe_getTEXTURES();
        int active  = GLStateManagerAccessor.canpipe_getActiveTexture();
        if (id != TEXTURES[active].binding) {
            TEXTURES[active].binding = id;
            GL33C.glBindTexture(target, id);
        }
    }

    @Override
    public void load(ResourceManager resourceManager) throws IOException {
        // do nothing
    }

    @Override
    public void close() {
        this.releaseId();
    }

}
