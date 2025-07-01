package fewizz.canpipe.pipeline;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.TextureCompareOp;
import fewizz.canpipe.TextureType;
import fewizz.canpipe.mixininterface.GpuDeviceExtended;
import fewizz.canpipe.mixininterface.GpuTextureExtended;
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

        FilterMode min = FilterMode.NEAREST;
        FilterMode mag = FilterMode.NEAREST;
        FilterMode mip = null;

        AddressMode u = AddressMode.REPEAT;
        AddressMode v = AddressMode.REPEAT;
        AddressMode r = null;

        boolean compare = false;
        TextureCompareOp compareOp = null;

        for (var paramsObject : JanksonUtils.listOfObjects(json, "texParams")) {
            String paramName = paramsObject.get(String.class, "name");
            String paramValue = paramsObject.get(String.class, "val");

            if (paramName.equals("TEXTURE_MIN_FILTER")) {
                switch (paramValue) {
                    case "NEAREST": { min = FilterMode.NEAREST; mip = null; break; }
                    case "LINEAR": { min = FilterMode.LINEAR; mip = null; break; }
                    case "NEAREST_MIPMAP_NEAREST": { min = FilterMode.NEAREST; mip = FilterMode.NEAREST; break; }
                    case "LINEAR_MIPMAP_NEAREST": { min = FilterMode.LINEAR; mip = FilterMode.NEAREST; break; }
                    case "NEAREST_MIPMAP_LINEAR": { min = FilterMode.NEAREST; mip = FilterMode.LINEAR; break; }
                    case "LINEAR_MIPMAP_LINEAR": { min = FilterMode.LINEAR; mip = FilterMode.LINEAR; break; }
                    default: throw new RuntimeException(paramValue);
                }
            }
            else if (paramName.equals("TEXTURE_MAG_FILTER")) {
                mag = switch (paramValue) {
                    case "NEAREST" -> FilterMode.NEAREST;
                    case "LINEAR" -> FilterMode.LINEAR;
                    default -> throw new RuntimeException(paramValue);
                };
            }
            else if (paramName.equals("TEXTURE_WRAP_S")) {
                u = switch (paramValue) {
                    case "CLAMP_TO_EDGE" -> AddressMode.CLAMP_TO_EDGE;
                    case "REPEAT" -> AddressMode.REPEAT;
                    default -> throw new RuntimeException(paramValue);
                };
            }
            else if (paramName.equals("TEXTURE_WRAP_T")) {
                v = switch (paramValue) {
                    case "CLAMP_TO_EDGE" -> AddressMode.CLAMP_TO_EDGE;
                    case "REPEAT" -> AddressMode.REPEAT;
                    default -> throw new RuntimeException(paramValue);
                };
            }
            else if (paramName.equals("TEXTURE_WRAP_R")) {
                r = switch (paramValue) {
                    case "CLAMP_TO_EDGE" -> AddressMode.CLAMP_TO_EDGE;
                    case "REPEAT" -> AddressMode.REPEAT;
                    default -> throw new RuntimeException(paramValue);
                };
            }
            else if (paramName.equals("TEXTURE_COMPARE_MODE")) {
                compare = switch (paramValue) {
                    case "NONE" -> false;
                    case "COMPARE_REF_TO_TEXTURE" -> true;
                    default -> throw new RuntimeException(paramValue);
                };
            }
            else if (paramName.equals("TEXTURE_COMPARE_FUNC")) {
                compareOp = switch(paramValue) {
                    case "NEVER" -> TextureCompareOp.NEVER;
                    case "LESS" -> TextureCompareOp.LESS;
                    case "LEQUAL" -> TextureCompareOp.LESS_OR_EQUAL;
                    case "NOTEQUAL" -> TextureCompareOp.NOT_EQUAL;
                    case "GEQUAL" -> TextureCompareOp.GREATER_OR_EQUAL;
                    case "ALWAYS" -> TextureCompareOp.ALWAYS;
                    default -> throw new RuntimeException(paramValue);
                };
            }
            else {
                CanPipe.LOGGER.warn("Unsupported texture param \""+paramName+"\" = \""+paramValue+"\"");
            }
        }

        texture.setTextureFilter(min, mag, false);
        ((GpuTextureExtended) texture).canpipe_setMipmapMode(mip);
        texture.setAddressMode(u, v);
        ((GpuTextureExtended) texture).canpipe_setAddressModeR(r);
        ((GpuTextureExtended) texture).canpipe_setCompareOp(compare ? compareOp : null);

        return texture;
    }

}
