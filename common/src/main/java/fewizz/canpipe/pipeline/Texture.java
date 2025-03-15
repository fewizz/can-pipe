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

    private record IntParam(int name, int value){};

    private Texture(
        ResourceLocation pipelineLocation, String name, Vector3i extent,
        int target, int internalFormat, int pixelFormat, int pixeDataType, int maxLod,
        List<IntParam> params
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
        if (this.isWidthWindowSizeDependent) {
            extent.x = mc.getWindow().getWidth();
        }
        if (this.isHeightWindowSizeDependent) {
            extent.y = mc.getWindow().getHeight();
        }

        bind();

        for (var p : params) {
            GlStateManager._texParameter(target, p.name, p.value);
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

        for (int lod = 0; lod <= this.maxLod; ++lod) {
            int w = this.extent.x >> lod;
            int h = this.extent.y >> lod;
            int d = this.extent.z >> lod;

            if (this.target == GL33C.GL_TEXTURE_2D) {
                GlStateManager._texImage2D(this.target, lod, this.internalFormat, w, h, 0, this.pixelFormat, this.pixelDataType, (IntBuffer) null);
            } else if (this.target == GL33C.GL_TEXTURE_3D) {
                GFX.glTexImage3D(this.target, lod, this.internalFormat, w, h, d, 0, this.pixelFormat, this.pixelDataType, (ByteBuffer) null);
            } else if (this.target == GL33C.GL_TEXTURE_2D_ARRAY) {
                GFX.glTexImage3D(this.target, lod, this.internalFormat, w, h, this.extent.z, 0, this.pixelFormat, this.pixelDataType, (ByteBuffer) null);
            } else if (this.target == GL33C.GL_TEXTURE_CUBE_MAP) {
                for (int face = 0; face < 6; ++face) {
                    GlStateManager._texImage2D(GL33C.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, lod, this.internalFormat, w, h, 0, this.pixelFormat, this.pixelDataType, (IntBuffer) null);
                }
            } else if (this.target == GL40C.GL_TEXTURE_CUBE_MAP_ARRAY) {
                /* "... depth must be a multiple of six indicating 6N layer-faces in the
                cube map array, otherwise the error INVALID_VALUE is generated." */
                GFX.glTexImage3D(GL40C.GL_TEXTURE_CUBE_MAP_ARRAY, lod, this.internalFormat, w, h, this.extent.z * 6, 0, this.pixelFormat, this.pixelDataType, (ByteBuffer) null);
            } else {
                throw new NotImplementedException();
            }
        }
    }

    public void onWindowSizeChanged(int width, int height) {
        boolean reallocate = false;
        if (this.isWidthWindowSizeDependent) {
            reallocate |= this.extent.x != width;
            this.extent.x = width;
        }
        if (this.isHeightWindowSizeDependent) {
            reallocate |= this.extent.y != height;
            this.extent.y = height;
        }
        if (reallocate) {
            allocate();
        }
    }

    public void bind() {
        GFX.glBindTexture(this.target, getId());
    }

    @Override
    public void close() {
        this.releaseId();
    }

    static Texture load(JsonObject json, ResourceLocation pipelineLocation) {
        String name = json.get(String.class, "name");

        int maxLod = json.getInt("lod", 0);
        int size = json.getInt("size", 0);

        Vector3i extent = new Vector3i(
            json.getInt("width", size),
            json.getInt("height", size),
            json.getInt("depth", 0)
        );

        String targetStr = json.get(String.class, "target");

        Function<String, Integer> glConst = (String constantName) -> {
            // Not 3.3, because GL_TEXTURE_CUBE_MAP_ARRAY is in 4.0
            try {
                return GL40C.class.getField("GL_"+constantName).getInt(null);
            } catch (Exception e) {
                throw new RuntimeException("Couldn't find GL constant \""+constantName+"\"", e);
            }
        };

        int target = targetStr != null ? glConst.apply(targetStr) : GL33C.GL_TEXTURE_2D;

        String internalFormatStr = json.get(String.class, "internalFormat");
        int internalFormat = internalFormatStr != null ? glConst.apply(internalFormatStr) : GL33C.GL_RGBA8;

        String pixelFormatStr = json.get(String.class, "pixelFormat");
        int pixelFormat = pixelFormatStr != null ? glConst.apply(pixelFormatStr) : GL33C.GL_RGBA;

        String pixelDataTypeStr = json.get(String.class, "pixelDataType");
        int pixelDataType = pixelDataTypeStr != null ? glConst.apply(pixelDataTypeStr) : GL33C.GL_UNSIGNED_BYTE;

        List<IntParam> params = new ArrayList<>();

        for (var paramsObject : JanksonUtils.listOfObjects(json, "texParams")) {
            int paramName = glConst.apply(paramsObject.get(String.class, "name"));
            int paramValue = glConst.apply(paramsObject.get(String.class, "val"));
            params.add(new IntParam(paramName, paramValue));
        }

        return new Texture(pipelineLocation, name, extent, target, internalFormat, pixelFormat,pixelDataType, maxLod, params);
    }

}
