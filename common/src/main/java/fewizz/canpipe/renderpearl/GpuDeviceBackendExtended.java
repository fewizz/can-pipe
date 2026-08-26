package fewizz.canpipe.renderpearl;

import java.util.OptionalDouble;

import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;

import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;

import net.minecraft.resources.Identifier;

public interface GpuDeviceBackendExtended extends GpuDeviceBackend {

    GpuSampler canpipe_createSampler(
        AddressMode u, AddressMode v, FilterMode min, FilterMode mag, int maxAnisotropy, OptionalDouble maxLod,
        AddressMode w, @Nullable CompareOp compareOp, boolean linearMipmap  // added
    );

    GpuTextureView canpipe_createTextureView(
        GpuTexture gpuTexture, int baseMip, int levelCount,
        int baseLayer, int layerCount  // added
    );

    GpuShaderModule canpipe_precompilePipelineModule(
        Identifier id,
        String shaderSource,
        ShaderType shaderType,
        TriConsumer<String, Identifier, String> onCompilationError
    );

}
