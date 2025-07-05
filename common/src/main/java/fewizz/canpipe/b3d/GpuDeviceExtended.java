package fewizz.canpipe.b3d;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;

import net.minecraft.resources.ResourceLocation;

public interface GpuDeviceExtended extends GpuDevice {

    CompiledRenderPipeline canpipe_compilePipeline(
        RenderPipeline pipeline,
        BiFunction<ResourceLocation, ShaderType, String> shaderSource,
        Consumer<String> onCompilationError
    );

    GpuTexture canpipe_createTexture(
        @Nullable String label, int usage, TextureFormat textureFormat, int w, int h, int depth, int maxLevel,
        TextureType type  // added
    );

    GpuTextureView canpipe_createTextureView(
        GpuTexture gpuTexture, int baseMip, int levelCount,
        int baseLayer, int layerCount // added
    );

}
