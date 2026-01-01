package fewizz.canpipe.b3d;

import java.util.OptionalDouble;

import org.apache.commons.lang3.function.TriConsumer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.resources.Identifier;

public interface GpuDeviceExtended extends GpuDevice {

    GpuSamplerExteneded canpie_createSampler(
		AddressMode u, AddressMode v, AddressMode w,
        FilterMode min, FilterMode mag, DepthTestFunction compareOp,
        int maxAnisotropy, OptionalDouble maxLod
	);

    void canpipe_precompilePipelineShaderModules(
        RenderPipeline pipeline,
        ShaderSource shaderSource,
        TriConsumer<String, Identifier, String> onCompilationError
    );

    GpuTextureView canpipe_createTextureView(
        GpuTexture gpuTexture, int baseMip, int levelCount,
        int baseLayer, int layerCount  // added
    );

}
