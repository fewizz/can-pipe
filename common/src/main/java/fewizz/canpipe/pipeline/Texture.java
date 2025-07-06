package fewizz.canpipe.pipeline;

import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.b3d.CompareOp;
import fewizz.canpipe.b3d.GpuDeviceExtended;
import fewizz.canpipe.b3d.GpuTextureExtended;
import fewizz.canpipe.b3d.TextureType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;


public class Texture extends AbstractTexture {
    private final Supplier<GpuTexture> gpuTextureSupplier;
    private final boolean recreateOnResize;

    private Texture(String name, boolean recreateOnResize, Supplier<GpuTexture> gpuTextureUpdater) {
        this.gpuTextureSupplier = gpuTextureUpdater;
        this.recreateOnResize = recreateOnResize;
        this.texture = this.gpuTextureSupplier.get();
        this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
    }

    void onWindowSizeChanged() {
        if (this.recreateOnResize) {
            this.close();
            this.texture = this.gpuTextureSupplier.get();
            this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
        }
    }

    static Texture load(JsonObject json, ResourceLocation pipelineLocation) {
        String name = json.get(String.class, "name");

        int maxLod = json.getInt("lod", 0);
        int size = json.getInt("size", 0);

        int width = json.getInt("width", size);
        int height = json.getInt("height", size);
        int depth = json.getInt("depth", 1);

        String targetStr = json.get(String.class, "target");
        String internalFormatStr = json.get(String.class, "internalFormat");

        FilterMode min = FilterMode.NEAREST;
        FilterMode mag = FilterMode.NEAREST;
        FilterMode mip = null;

        AddressMode u = AddressMode.REPEAT;
        AddressMode v = AddressMode.REPEAT;
        AddressMode r = null;

        boolean compare = false;
        CompareOp compareOp = null;

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
                    case "NEVER" -> CompareOp.NEVER;
                    case "LESS" -> CompareOp.LESS;
                    case "LEQUAL" -> CompareOp.LESS_OR_EQUAL;
                    case "NOTEQUAL" -> CompareOp.NOT_EQUAL;
                    case "GEQUAL" -> CompareOp.GREATER_OR_EQUAL;
                    case "ALWAYS" -> CompareOp.ALWAYS;
                    default -> throw new RuntimeException(paramValue);
                };
            }
            else {
                CanPipe.LOGGER.warn("Unsupported texture param \""+paramName+"\" = \""+paramValue+"\"");
            }
        }

        try {
            TextureFormat textureFormat = TextureFormat.valueOf(internalFormatStr);
            TextureType textureType = switch (targetStr) {
                case "TEXTURE_2D" -> TextureType.TYPE_2D;
                case "TEXTURE_2D_ARRAY" -> TextureType.TYPE_2D_ARRAY;
                case "TEXTURE_CUBE_MAP" -> TextureType.TYPE_CUBE_MAP;
                default -> throw new RuntimeException("Unsupported texture type \""+targetStr+"\"");
            };

            final FilterMode minFilter = min;
            final FilterMode magFilter = mag;
            final FilterMode mipFilter = mip;
            final AddressMode uAddressMode = u;
            final AddressMode vAddressMode = v;
            final AddressMode rAddressMode = r;
            final CompareOp depthCompareOp = compare ? compareOp : null;

            boolean recreateOnResize = width == 0 || height == 0;

            return new Texture(name, recreateOnResize, () -> {
                int w = width, h = height;
                var window = Minecraft.getInstance().getWindow();
                if (width <= 0) { w = window.getWidth(); }
                if (height <= 0) { h = window.getHeight(); }

                GpuTexture texture = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTexture(
                    name,
                    GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                    textureFormat,
                    w, h, depth, maxLod+1,
                    textureType
                );

                texture.setTextureFilter(minFilter, magFilter, false);
                ((GpuTextureExtended) texture).canpipe_setMipmapMode(mipFilter);
                texture.setAddressMode(uAddressMode, vAddressMode);
                ((GpuTextureExtended) texture).canpipe_setAddressModeR(rAddressMode);
                ((GpuTextureExtended) texture).canpipe_setCompareOp(depthCompareOp);
                return texture;
            });
        } catch (Exception e) {
            throw new RuntimeException("Couldn't create texture \""+name+"\"", e);
        }
    }

}
