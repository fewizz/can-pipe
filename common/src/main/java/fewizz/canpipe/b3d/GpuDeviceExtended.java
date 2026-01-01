package fewizz.canpipe.b3d;

import org.apache.commons.lang3.function.TriConsumer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.resources.Identifier;

public interface GpuDeviceExtended extends GpuDevice {

    void canpipe_precompilePipelineShaderModules(
        RenderPipeline pipeline,
        ShaderSource shaderSource,
        TriConsumer<String, Identifier, String> onCompilationError
    );

    GpuTextureView canpipe_createTextureView(
        GpuTexture gpuTexture, int baseMip, int levelCount,
        int baseLayer, int layerCount // added
    );

}
