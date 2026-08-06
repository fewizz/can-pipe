package fewizz.canpipe.pipeline;

import java.util.OptionalDouble;
import java.util.function.Supplier;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.b3d.GpuDeviceExtended;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;


public class Texture extends AbstractTexture {
    private final Supplier<GpuTexture> gpuTextureSupplier;
    private final boolean recreateOnResize;
    private final boolean closeSampler;

    protected Texture(String name, GpuSampler sampler, boolean closeSampler, boolean recreateOnResize, Supplier<GpuTexture> gpuTextureUpdater) {
        this.gpuTextureSupplier = gpuTextureUpdater;
        this.recreateOnResize = recreateOnResize;
        this.sampler = sampler;
        this.closeSampler = closeSampler;
        this.texture = this.gpuTextureSupplier.get();
        this.textureView = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
            this.texture, 0, this.texture.getMipLevels(), 0, this.texture.getDepthOrLayers()
        );
    }

    @Override
    public void close() {
        super.close();
        if (closeSampler) { this.sampler.close(); }
    }

    void onWindowSizeChanged() {
        if (this.recreateOnResize) {
            this.texture.close();
            this.textureView.close();
            this.texture = this.gpuTextureSupplier.get();
            this.textureView = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                this.texture, 0, this.texture.getMipLevels(), 0, this.texture.getDepthOrLayers()
            );
        }
    }

    static Texture load(JsonObject json, Identifier pipelineLocation) {
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
        GpuFormat textureFormat = switch (internalFormatStr) {
            case null -> GpuFormat.RGBA8_UNORM;
            case "DEPTH_COMPONENT" -> GpuFormat.D32_FLOAT;
            case "DEPTH_COMPONENT32" -> GpuFormat.D32_FLOAT;
            case "R8" -> GpuFormat.R8_UNORM;
            case "R8_SNORM" -> GpuFormat.R8_SNORM;
            case "R16" -> GpuFormat.R16_UNORM;
            case "R16_SNORM" -> GpuFormat.R16_SNORM;
            case "R16F" -> GpuFormat.R16_FLOAT;
            case "R32F" -> GpuFormat.R32_FLOAT;
            case "RG8" -> GpuFormat.RG8_UNORM;
            case "RG8_SNORM" -> GpuFormat.RG8_SNORM;
            case "RG16" -> GpuFormat.RG16_UNORM;
            case "RG16_SNORM" -> GpuFormat.RG16_SNORM;
            case "RG16F" -> GpuFormat.RG16_FLOAT;
            case "RG32F" -> GpuFormat.RG32_FLOAT;
            case "RGB8" -> GpuFormat.RGBA8_UNORM;                           // not RGB8_UNORM
            case "RGB8_SNORM" -> GpuFormat.RGBA8_SNORM;                     // not RGB8_SNORM
            case "RGB16" -> GpuFormat.RGBA16_UNORM;                         // not RGB16_UNORM
            case "RGB16_SNORM" -> GpuFormat.RGBA16_SNORM;                   // not RGB16_SNORM
            case "RGB32UI" -> GpuFormat.RGBA32_UINT;                        // not RGB32_UINT
            case "RGB16F" -> GpuFormat.RGBA16_FLOAT;                        // not RGB16_SFLOAT
            case "RGB32F" -> GpuFormat.RGBA32_FLOAT;                        // not RGB32_SFLOAT
            case "R11F_G11F_B10F" -> GpuFormat.RG11B10_FLOAT;
            case "RGBA8" -> GpuFormat.RGBA8_UNORM;
            case "RGBA8_SNORM" -> GpuFormat.RGBA8_SNORM;
            case "RGBA12" -> GpuFormat.RGBA16_UNORM;   // have to provide RGBA12 instead of RGBA12
            case "RGBA16" -> GpuFormat.RGBA16_UNORM;
            case "RGBA32UI" -> GpuFormat.RGBA32_UINT;
            case "RGBA16F" -> GpuFormat.RGBA16_FLOAT;
            case "RGBA32F" -> GpuFormat.RGBA32_FLOAT;
            default -> { throw new RuntimeException(internalFormatStr); }
        };

        FilterMode min = FilterMode.NEAREST;
        FilterMode mag = FilterMode.NEAREST;
        boolean linearMip = false;

        AddressMode u = AddressMode.REPEAT;
        AddressMode v = AddressMode.REPEAT;
        AddressMode w = AddressMode.REPEAT;

        boolean compare = false;
        CompareOp compareOp = null;

        for (var paramsJson : JanksonUtils.listOfObjects(json, "texParams")) {
            String paramName = paramsJson.get(String.class, "name");
            String paramValue = paramsJson.get(String.class, "val");

            if (paramName.equals("TEXTURE_MIN_FILTER")) {
                switch (paramValue) {
                    case "NEAREST": { min = FilterMode.NEAREST; break; }
                    case "NEAREST_MIPMAP_NEAREST": { min = FilterMode.NEAREST; break; }

                    case "LINEAR": { min = FilterMode.LINEAR; break; }
                    case "LINEAR_MIPMAP_NEAREST": { min = FilterMode.LINEAR; break; }

                    case "NEAREST_MIPMAP_LINEAR": { min = FilterMode.NEAREST; linearMip = true; break; }
                    case "LINEAR_MIPMAP_LINEAR": { min = FilterMode.LINEAR; linearMip = true; break; }

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
                    case "EQUAL" -> CompareOp.EQUAL;
                    case "NOTEQUAL" -> CompareOp.NOT_EQUAL;
                    case "LESS" -> CompareOp.LESS_THAN;
                    case "LEQUAL" -> CompareOp.LESS_THAN_OR_EQUAL;
                    case "GREATER" -> CompareOp.GREATER_THAN;
                    case "GEQUAL" -> CompareOp.GREATER_THAN_OR_EQUAL;
                    case "ALWAYS" -> CompareOp.ALWAYS_PASS;
                    case "NEVER" -> CompareOp.NEVER_PASS;
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

            GpuSampler sampler = (GpuSampler) ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createSampler(
                v, u, min, mag, 1, OptionalDouble.of(maxLod),
                w, compare ? compareOp : null, linearMip
            );

            int usage = GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING;
            int depthOrLayers = depth;

            if (targetStr.equals("TEXTURE_CUBE_MAP")) {
                usage |= GpuTexture.USAGE_CUBEMAP_COMPATIBLE;
                depthOrLayers *= 6;
            }

            int finalUsage = usage;
            int finalDepthOrLayer = depthOrLayers;

            boolean recreateOnResize = width == 0 || height == 0;

            return new Texture(name, sampler, true /* we use custom, uncached samplers */, recreateOnResize, () -> {
                int newWidth = width; int newHeight = height;

                var window = Minecraft.getInstance().getWindow();
                if (newWidth <= 0) { newWidth = window.getWidth(); }
                if (newHeight <= 0) { newHeight = window.getHeight(); }

                return RenderSystem.getDevice().createTexture(name, finalUsage, textureFormat, newWidth, newHeight, finalDepthOrLayer, maxLod+1);
            });
        } catch (Exception e) {
            throw new RuntimeException("Couldn't create texture \""+name+"\"", e);
        }
    }

}
