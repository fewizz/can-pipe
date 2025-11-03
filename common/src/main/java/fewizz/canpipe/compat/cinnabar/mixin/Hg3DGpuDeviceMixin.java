package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.function.BiFunction;

import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.GpuDeviceExtended;
import graphics.cinnabar.core.hg3d.Hg3DGpuDevice;
import net.minecraft.resources.ResourceLocation;

@Mixin(Hg3DGpuDevice.class)
public abstract class Hg3DGpuDeviceMixin implements GpuDeviceExtended {

    @Unique private int canpipe_pendingTextureViewBaseLayer = -1;
    @Unique private int canpipe_pendingTextureViewLayerCount = -1;

    @Override
    public CompiledRenderPipeline canpipe_precompilePipeline(
        RenderPipeline pipeline,
        BiFunction<ResourceLocation, ShaderType, String> shaderSource,
        TriConsumer<String, ResourceLocation, String> onCompilationError
    ) {
        return this.precompilePipeline(
            pipeline,
            shaderSource
        );
    }

    @Override
    public GpuTextureView canpipe_createTextureView(
        GpuTexture gpuTexture, int baseMip, int levelCount,
        int baseLayer, int layerCount // added
    ) {
        try {
            this.canpipe_pendingTextureViewBaseLayer = baseLayer;
            this.canpipe_pendingTextureViewLayerCount = layerCount;
            return this.createTextureView(gpuTexture, baseMip, levelCount);
        } finally {
            this.canpipe_pendingTextureViewBaseLayer = -1;
            this.canpipe_pendingTextureViewLayerCount = -1;
        }
    }
    
}
