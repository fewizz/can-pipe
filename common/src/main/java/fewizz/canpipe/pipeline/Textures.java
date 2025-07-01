package fewizz.canpipe.pipeline;

import org.lwjgl.opengl.GL33C;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.TextureType;
import fewizz.canpipe.mixin.m02_texture_targets.GlStateManagerAccessor;
import fewizz.canpipe.mixininterface.GpuDeviceExtended;
import net.minecraft.resources.ResourceLocation;


public class Textures {

    static GpuTexture load(JsonObject json, ResourceLocation pipelineLocation, int defaultWidth, int defaultHeight) {
        String name = json.get(String.class, "name");

        int maxLod = json.getInt("lod", 0);
        int size = json.getInt("size", 0);

        int width = json.getInt("width", size != 0 ? size : defaultWidth);
        int height = json.getInt("height", size != 0 ? size : defaultHeight);
        int depth = json.getInt("depth", 1);

        String targetStr = json.get(String.class, "target");
        TextureType textureType = switch (targetStr) {
            case "TEXTURE_2D" -> TextureType.TYPE_2D;
            case "TEXTURE_2D_ARRAY" -> TextureType.TYPE_2D_ARRAY;
            case "TEXTURE_CUBE_MAP" -> TextureType.TYPE_CUBE_MAP;
            default -> throw new RuntimeException("Unsupported texture type \""+targetStr+"\"");
        };

        /*Function<String, Integer> glConst = (String constantName) -> {
            // Not 3.3, because GL_TEXTURE_CUBE_MAP_ARRAY is in 4.0
            try {
                return GL40C.class.getField("GL_"+constantName).getInt(null);
            } catch (Exception e) {
                throw new RuntimeException("Couldn't find GL constant \""+constantName+"\"", e);
            }
        };*/

        String internalFormatStr = json.get(String.class, "internalFormat");
        /*
        if (internalFormatStr == null) { internalFormatStr = "RGBA8"; }
        int internalFormat = glConst.apply(internalFormatStr);

        int target = targetStr != null ? glConst.apply(targetStr) : GL33C.GL_TEXTURE_2D;

        String pixelFormatStr = json.get(String.class, "pixelFormat");
        int pixelFormat = pixelFormatStr != null ? glConst.apply(pixelFormatStr) : GL33C.GL_RGBA;

        String pixelDataTypeStr = json.get(String.class, "pixelDataType");
        int pixelDataType = pixelDataTypeStr != null ? glConst.apply(pixelDataTypeStr) : GL33C.GL_UNSIGNED_BYTE;
        */

        if (internalFormatStr.equals("DEPTH_COMPONENT32")) {
            internalFormatStr = "DEPTH32";
        }

        GpuTexture texture;
        try {
            texture = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTexture(
                name,
                GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.valueOf(internalFormatStr),
                width, height, depth, maxLod+1,
                textureType
            );
        } catch (Exception e) {
            throw new RuntimeException("Couldn't create texture \""+name+"\"", e);
        }

        GlTexture glTexture = (GlTexture) texture;
        int target = GlStateManagerAccessor.canpipe_getTextureTarget(glTexture.glId());
        glTexture.flushModeChanges(target);

        /*FilterMode min = FilterMode.NEAREST;
        FilterMode mag = FilterMode.NEAREST;
        AddressMode u = AddressMode.REPEAT;
        AddressMode v = AddressMode.REPEAT;*/

        for (var paramsObject : JanksonUtils.listOfObjects(json, "texParams")) {
            String paramName = paramsObject.get(String.class, "name");
            String paramValue = paramsObject.get(String.class, "val");

            if (paramName.equals("TEXTURE_MIN_FILTER")) {
                GlStateManager._texParameter(
                    target,
                    GL33C.GL_TEXTURE_MIN_FILTER,
                    switch (paramValue) {
                        case "NEAREST" -> GL33C.GL_NEAREST;
                        case "LINEAR" -> GL33C.GL_LINEAR;
                        case "NEAREST_MIPMAP_NEAREST" -> GL33C.GL_NEAREST_MIPMAP_NEAREST;
                        case "LINEAR_MIPMAP_NEAREST" -> GL33C.GL_LINEAR_MIPMAP_NEAREST;
                        case "NEAREST_MIPMAP_LINEAR" -> GL33C.GL_NEAREST_MIPMAP_LINEAR;
                        case "LINEAR_MIPMAP_LINEAR" -> GL33C.GL_LINEAR_MIPMAP_LINEAR;
                        default -> throw new RuntimeException(paramValue);
                    }
                );
                /*if (paramName.equals("LINEAR") || paramName.equals("LINEAR_MIPMAP_NEAREST") || paramName.equals("LINEAR_MIPMAP_LINEAR")) {
                    min = FilterMode.LINEAR;
                }*/
            }
            else if (paramName.equals("TEXTURE_MAG_FILTER")) {
                GlStateManager._texParameter(
                    target,
                    GL33C.GL_TEXTURE_MAG_FILTER,
                    switch (paramValue) {
                        case "NEAREST" -> GL33C.GL_NEAREST;
                        case "LINEAR" -> GL33C.GL_LINEAR;
                        default -> throw new RuntimeException(paramValue);
                    }
                );
                /*if (paramName.equals("LINEAR")) {
                    mag = FilterMode.LINEAR;
                }*/
            }
            else if (paramName.equals("TEXTURE_WRAP_S")) {
                GlStateManager._texParameter(
                    target,
                    GL33C.GL_TEXTURE_WRAP_S,
                    switch (paramValue) {
                        case "CLAMP_TO_EDGE" -> GL33C.GL_CLAMP_TO_EDGE;
                        case "REPEAT" -> GL33C.GL_REPEAT;
                        default -> throw new RuntimeException(paramValue);
                    }
                );
                /*u = switch (paramValue) {
                    case "CLAMP_TO_EDGE" -> AddressMode.CLAMP_TO_EDGE;
                    case "REPEAT" -> AddressMode.REPEAT;
                    default -> throw new RuntimeException(paramValue);
                };*/
            }
            else if (paramName.equals("TEXTURE_WRAP_T")) {
                GlStateManager._texParameter(
                    target,
                    GL33C.GL_TEXTURE_WRAP_T,
                    switch (paramValue) {
                        case "CLAMP_TO_EDGE" -> GL33C.GL_CLAMP_TO_EDGE;
                        case "REPEAT" -> GL33C.GL_REPEAT;
                        default -> throw new RuntimeException(paramValue);
                    }
                );
                /*v = switch (paramValue) {
                    case "CLAMP_TO_EDGE" -> AddressMode.CLAMP_TO_EDGE;
                    case "REPEAT" -> AddressMode.REPEAT;
                    default -> throw new RuntimeException(paramValue);
                };*/
            }
            else if (paramName.equals("TEXTURE_WRAP_R")) {
                GlStateManager._texParameter(
                    target,
                    GL33C.GL_TEXTURE_WRAP_R,
                    switch (paramValue) {
                        case "CLAMP_TO_EDGE" -> GL33C.GL_CLAMP_TO_EDGE;
                        case "REPEAT" -> GL33C.GL_REPEAT;
                        default -> throw new RuntimeException(paramValue);
                    }
                );
            }
            else if (paramName.equals("TEXTURE_COMPARE_MODE")) {
                GlStateManager._texParameter(
                    target,
                    GL33C.GL_TEXTURE_COMPARE_MODE,
                    switch (paramValue) {
                        case "NONE" -> GL33C.GL_NONE;
                        case "COMPARE_REF_TO_TEXTURE" -> GL33C.GL_COMPARE_REF_TO_TEXTURE;
                        default -> throw new RuntimeException(paramValue);
                    }
                );
            }
            else if (paramName.equals("TEXTURE_COMPARE_FUNC")) {
                GlStateManager._texParameter(
                    target,
                    GL33C.GL_TEXTURE_COMPARE_FUNC,
                    switch (paramValue) {
                        case "LEQUAL" -> GL33C.GL_LEQUAL;
                        default -> throw new RuntimeException(paramValue);
                    }
                );
            }
            else {
                CanPipe.LOGGER.warn("Unsupported texture param \""+paramName+"\" = \""+paramValue+"\"");
            }
        }

        // texture.setTextureFilter(min, mag, true);
        // texture.setAddressMode(u, v);

        return texture;
    }

}
