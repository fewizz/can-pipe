package fewizz.canpipe.pipeline;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.NotImplementedException;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL40C;

import com.mojang.blaze3d.platform.GlStateManager;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.GFX;
import fewizz.canpipe.JanksonUtils;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
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

    private Texture(
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
        GFX.glObjectLabel(GL33C.GL_TEXTURE, getId(), pipelineLocation.toString()+"-"+name);
    }

    private void allocate() {
        bind();

        for (int i = 0; i <= maxLod; ++i) {
            int w = this.extent.x >> i;
            int h = this.extent.y >> i;

            if (target == GL33C.GL_TEXTURE_2D) {
                GlStateManager._texImage2D(target, i, internalFormat, w, h, 0, pixelFormat, pixelDataType, (IntBuffer) null);
            } else if (target == GL33C.GL_TEXTURE_3D) {
                GFX.glTexImage3D(target, i, internalFormat, w, h, this.extent.z >> i, 0, pixelFormat, pixelDataType, (ByteBuffer) null);
            } else if (target == GL33C.GL_TEXTURE_2D_ARRAY) {
                GFX.glTexImage3D(target, i, internalFormat, w, h, this.extent.z, 0, pixelFormat, pixelDataType, (ByteBuffer) null);
            } else if (target == GL33C.GL_TEXTURE_CUBE_MAP) {
                for (int face = 0; face < 6; ++face) {
                    GlStateManager._texImage2D(GL33C.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, i, internalFormat, w, h, 0, pixelFormat, pixelDataType, (IntBuffer) null);
                }
            } else if (target == GL40C.GL_TEXTURE_CUBE_MAP_ARRAY) {
                /* "... depth must be a multiple of six indicating 6N layer-faces in the
                cube map array, otherwise the error INVALID_VALUE is generated." */
                GFX.glTexImage3D(GL40C.GL_TEXTURE_CUBE_MAP_ARRAY, i, internalFormat, w, h, this.extent.z * 6, 0, pixelFormat, pixelDataType, (ByteBuffer) null);
            } else {
                throw new NotImplementedException();
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

    public void bind() {
        GFX.glBindTexture(target, getId());
    }

    @Override
    public void close() {
        this.releaseId();
    }

    static Texture load(
        JsonObject json,
        ResourceLocation pipelineLocation
    ) {
        String name = json.get(String.class, "name");

        int maxLod = json.getInt("lod", 0);
        int depth = json.getInt("depth", 0);
        int size = json.getInt("size", 0);
        int width = json.getInt("width", size);
        int height = json.getInt("height", size);

        String targetStr = json.get(String.class, "target");

        Function<String, Integer> glConstantCode = (String constantName) -> {
            // Not 3.3, because GL_TEXTURE_CUBE_MAP_ARRAY is in 4.0
            try {
                return GL40C.class.getField("GL_"+constantName).getInt(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        int target = targetStr != null ? glConstantCode.apply(targetStr) : GL33C.GL_TEXTURE_2D;

        String internalFormatStr = json.get(String.class, "internalFormat");
        int internalFormat = internalFormatStr != null ? glConstantCode.apply(internalFormatStr) : GL33C.GL_RGBA8;

        String pixelFormatStr = json.get(String.class, "pixelFormat");
        int pixelFormat = pixelFormatStr != null ? glConstantCode.apply(pixelFormatStr) : GL33C.GL_RGBA;

        String pixelDataTypeStr = json.get(String.class, "pixelDataType");
        int pixelDataType = pixelDataTypeStr != null ? glConstantCode.apply(pixelDataTypeStr) : GL33C.GL_UNSIGNED_BYTE;

        List<IntIntPair> params = new ArrayList<>();

        for (var paramsO : JanksonUtils.listOfObjects(json, "texParams")) {
            int name0 = glConstantCode.apply(paramsO.get(String.class, "name"));
            int value = glConstantCode.apply(paramsO.get(String.class, "val"));
            params.add(IntIntImmutablePair.of(name0, value));
        }

        return new Texture(
            pipelineLocation, name,
            new Vector3i(width, height, depth),
            target, internalFormat, pixelFormat,
            pixelDataType, maxLod, params.toArray(new IntIntPair[]{})
        );
    }

}
