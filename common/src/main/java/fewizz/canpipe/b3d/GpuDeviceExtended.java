package fewizz.canpipe.b3d;

import java.util.OptionalDouble;

import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.resources.Identifier;

public interface GpuDeviceExtended {

    GpuSampler canpipe_createSampler(
        AddressMode u, AddressMode v, FilterMode min, FilterMode mag, int maxAnisotropy, OptionalDouble maxLod,
        AddressMode w, @Nullable CompareOp compareOp, boolean linearMipmap  // added
    );

    GpuTextureView canpipe_createTextureView(
        GpuTexture gpuTexture, int baseMip, int levelCount,
        int baseLayer, int layerCount  // added
    );

    GpuShaderModule canpipe_precompileShaderModule(
        Identifier id,
        String shaderSource,
        ShaderType shaderType,
        TriConsumer<String, Identifier, String> onCompilationError
    );

}
