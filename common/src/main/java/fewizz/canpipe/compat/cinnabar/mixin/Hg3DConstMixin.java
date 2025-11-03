package fewizz.canpipe.compat.cinnabar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.mojang.blaze3d.textures.TextureFormat;

import graphics.cinnabar.api.hg.enums.HgFormat;
import graphics.cinnabar.core.hg3d.Hg3DConst;

@Mixin(Hg3DConst.class)
public class Hg3DConstMixin {

    /**
     * @author fewizz
     * @reason for fun
     */
    @Overwrite
    public static HgFormat format(TextureFormat original) {
        if (original == TextureFormat.RGBA8) {
            return HgFormat.RGBA8_UNORM;
        }
        if (original == TextureFormat.RED8) {
            return HgFormat.R8_UNORM;
        }
        if (original == TextureFormat.RED8I) {
            return HgFormat.R8_SINT;
        }
        if (original == TextureFormat.DEPTH32) {
            return HgFormat.D32_SFLOAT;
        }

        return switch (original.name()) {
            case "R8_UNORM" -> HgFormat.R8_UNORM;
            case "R8_SNORM" -> HgFormat.R8_SNORM;
            case "R16_UNORM" -> HgFormat.R16_UNORM;
            case "R16_SNORM" -> HgFormat.R16_SNORM;
            case "R16_SFLOAT" -> HgFormat.R32_SFLOAT;  // TODO
            case "R32_SFLOAT" -> HgFormat.R32_SFLOAT;
            case "RG8_UNORM" -> HgFormat.RG8_UNORM;
            case "RG8_SNORM" -> HgFormat.RG8_SNORM;
            case "RG16_UNORM" -> HgFormat.RG16_UNORM;
            case "RG16_SNORM" -> HgFormat.RG16_SNORM;
            case "RG16_SFLOAT" -> HgFormat.RG32_SFLOAT;  // TODO
            case "RG32_SFLOAT" -> HgFormat.RG32_SFLOAT;
            case "RGB8_UNORM" -> HgFormat.RGB8_UNORM;
            case "RGB8_SNORM" -> HgFormat.RGB8_SNORM;
            case "RGB16_UNORM" -> HgFormat.RGB16_UNORM;
            case "RGB16_SNORM" -> HgFormat.RGB16_SNORM;
            case "RGB32_UINT" -> HgFormat.RGB32_UINT;
            case "RGB16_SFLOAT" -> HgFormat.RGB32_SFLOAT;  // TODO
            case "RGB32_SFLOAT" -> HgFormat.RGB32_SFLOAT;
            case "B10G11R11_UFLOAT_PACK32" -> throw new RuntimeException();
            case "RGBA8_SNORM" -> HgFormat.RGBA8_SNORM;
            case "R12X4G12X4B12X4A12X4_UNORM_4PACK16" -> throw new RuntimeException();
            case "RGBA16_UNORM" -> HgFormat.RGBA16_UNORM;
            case "RGBA32_UINT" -> HgFormat.RGBA32_UINT;
            case "RGBA16_SFLOAT" -> HgFormat.RGBA32_SFLOAT;  // TODO
            case "RGBA32_SFLOAT" -> HgFormat.RGBA32_SFLOAT;
            default -> throw new RuntimeException();
        };
    }

}
