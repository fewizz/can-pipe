package fewizz.canpipe.pipeline;

import java.util.function.Supplier;

import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.b3d.GpuDeviceExtended;
import fewizz.canpipe.b3d.GpuTextureExtended;
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
        this.textureView = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
            this.texture, 0, this.texture.getMipLevels(), 0, this.texture.getDepthOrLayers()
        );
    }

    void onWindowSizeChanged() {
        if (this.recreateOnResize) {
            this.close();
            this.texture = this.gpuTextureSupplier.get();
            this.textureView = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                this.texture, 0, this.texture.getMipLevels(), 0, this.texture.getDepthOrLayers()
            );
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
        if (targetStr == null) {
            targetStr = "TEXTURE_2D";
        }

        String internalFormatStr = json.get(String.class, "internalFormat");
        TextureFormat textureFormat = switch (internalFormatStr) {
            case null -> TextureFormat.RGBA8;
            case "DEPTH_COMPONENT" -> TextureFormat.DEPTH32;
            case "DEPTH_COMPONENT32" -> TextureFormat.DEPTH32;
            case "RED8" -> TextureFormat.RED8;
            case "R8" -> TextureFormat.valueOf("R8_UNORM");
            case "R8_SNORM" -> TextureFormat.valueOf("R8_SNORM");
            case "R16" -> TextureFormat.valueOf("R16_UNORM");
            case "R16_SNORM" -> TextureFormat.valueOf("R16_SNORM");
            case "R16F" -> TextureFormat.valueOf("R16_SFLOAT");
            case "R32F" -> TextureFormat.valueOf("R32_SFLOAT");
            case "RG8" -> TextureFormat.valueOf("RG8_UNORM");
            case "RG8_SNORM" -> TextureFormat.valueOf("RG8_SNORM");
            case "RG16" -> TextureFormat.valueOf("RG16_UNORM");
            case "RG16_SNORM" -> TextureFormat.valueOf("RG16_SNORM");
            case "RG16F" -> TextureFormat.valueOf("RG16_SFLOAT");
            case "RG32F" -> TextureFormat.valueOf("RG32_SFLOAT");
            case "RGB8" -> TextureFormat.RGBA8;                           // not RGB8_UNORM
            case "RGB8_SNORM" -> TextureFormat.valueOf("RGBA8_SNORM");    // not RGB8_SNORM
            case "RGB16" -> TextureFormat.valueOf("RGBA16_UNORM");        // not RGB16_UNORM
            case "RGB16_SNORM" -> TextureFormat.valueOf("RGBA16_SNORM");  // not RGB16_SNORM
            case "RGB32UI" -> TextureFormat.valueOf("RGBA32_UINT");       // not RGB32_UINT
            case "RGB16F" -> TextureFormat.valueOf("RGBA16_SFLOAT");      // not RGB16_SFLOAT
            case "RGB32F" -> TextureFormat.valueOf("RGBA32_SFLOAT");      // not RGB32_SFLOAT
            case "R11F_G11F_B10F" -> TextureFormat.valueOf("B10G11R11_UFLOAT_PACK32");
            case "RGBA8" -> TextureFormat.RGBA8;
            case "RGBA8_SNORM" -> TextureFormat.valueOf("RGBA8_SNORM");
            case "RGBA12" -> TextureFormat.valueOf("R12X4G12X4B12X4A12X4_UNORM_4PACK16");
            case "RGBA16" -> TextureFormat.valueOf("RGBA16_UNORM");
            case "RGBA32UI" -> TextureFormat.valueOf("RGBA32_UINT");
            case "RGBA16F" -> TextureFormat.valueOf("RGBA16_SFLOAT");
            case "RGBA32F" -> TextureFormat.valueOf("RGBA32_SFLOAT");
            default -> { throw new RuntimeException(internalFormatStr); }
        };

        FilterMode min = FilterMode.NEAREST;
        FilterMode mag = FilterMode.NEAREST;
        FilterMode mip = null;

        AddressMode u = AddressMode.REPEAT;
        AddressMode v = AddressMode.REPEAT;
        AddressMode w = null;

        boolean compare = false;
        DepthTestFunction compareOp = null;

        for (var paramsJson : JanksonUtils.listOfObjects(json, "texParams")) {
            String paramName = paramsJson.get(String.class, "name");
            String paramValue = paramsJson.get(String.class, "val");

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
                w = switch (paramValue) {
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
                    case "EQUAL" -> DepthTestFunction.EQUAL_DEPTH_TEST;
                    case "LESS" -> DepthTestFunction.LESS_DEPTH_TEST;
                    case "LEQUAL" -> DepthTestFunction.LEQUAL_DEPTH_TEST;
                    case "GREATER" -> DepthTestFunction.GREATER_DEPTH_TEST;
                    case "ALWAYS" -> DepthTestFunction.NO_DEPTH_TEST;
                    // case "NEVER" -> ;
                    // case "NOTEQUAL" -> ;
                    // case "GEQUAL" -> ;
                    default -> throw new RuntimeException(paramValue);
                };
            }
            else {
                CanPipe.LOGGER.warn("Unsupported texture param \""+paramName+"\" = \""+paramValue+"\"");
            }
        }

        try {
            if (targetStr.equals("TEXTURE_2D_ARRAY") && depth <= 1) {
                CanPipe.LOGGER.warn("Texture \""+name+"\" type is TEXTURE_2D_ARRAY, but depth="+depth);
            }

            boolean cubeMapCompatible = targetStr.equals("TEXTURE_CUBE_MAP");

            final FilterMode minFilter = min;
            final FilterMode magFilter = mag;
            final FilterMode mipFilter = mip;
            final AddressMode addressModeU = u;
            final AddressMode addressModeV = v;
            final AddressMode addressModeW = w;
            final DepthTestFunction depthCompareOp = compare ? compareOp : null;

            boolean recreateOnResize = width == 0 || height == 0;

            return new Texture(name, recreateOnResize, () -> {
                int newWidth = width; int newHeight = height;

                var window = Minecraft.getInstance().getWindow();
                if (newWidth <= 0) { newWidth = window.getWidth(); }
                if (newHeight <= 0) { newHeight = window.getHeight(); }

                int usage = GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING;
                int depthOrLayers = depth;

                if (cubeMapCompatible) {
                    usage |= GpuTexture.USAGE_CUBEMAP_COMPATIBLE;
                    depthOrLayers *= 6;
                }

                GpuTexture texture = RenderSystem.getDevice().createTexture(
                    name, usage,
                    textureFormat,
                    newWidth, newHeight, depthOrLayers, maxLod+1
                );

                texture.setTextureFilter(minFilter, magFilter, false);
                ((GpuTextureExtended) texture).canpipe_setMipmapMode(mipFilter);
                texture.setAddressMode(addressModeU, addressModeV);
                ((GpuTextureExtended) texture).canpipe_setAddressModeW(addressModeW);
                ((GpuTextureExtended) texture).canpipe_setCompareOp(depthCompareOp);
                return texture;
            });
        } catch (Exception e) {
            throw new RuntimeException("Couldn't create texture \""+name+"\"", e);
        }
    }

}
