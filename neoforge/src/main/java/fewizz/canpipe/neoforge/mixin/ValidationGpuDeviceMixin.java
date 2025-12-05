package fewizz.canpipe.neoforge.mixin;

import java.util.function.BiFunction;

import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.GpuDeviceExtended;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuDevice;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuTexture;

@Mixin(ValidationGpuDevice.class)
public abstract class ValidationGpuDeviceMixin implements GpuDeviceExtended {

    @Shadow @Final protected GpuDevice realDevice;

    @Override
    public void canpipe_precompilePipelineShaderModules(
        RenderPipeline pipeline,
        BiFunction<ResourceLocation, ShaderType, String> shaderSource,
        TriConsumer<String, ResourceLocation, String> onCompilationError
    ) {
        ((GpuDeviceExtended) this.realDevice).canpipe_precompilePipelineShaderModules(
            pipeline, shaderSource, onCompilationError
        );
    }

    @Override
    public GpuTextureView canpipe_createTextureView(
        GpuTexture gpuTexture, int baseMip, int levelCount,
        int baseLayer, int layerCount // added
    ) {
        if (!(gpuTexture instanceof ValidationGpuTexture validationTexture)) {
            throw new IllegalArgumentException();
        }
        return ((GpuDeviceExtended) this.realDevice).canpipe_createTextureView(
            validationTexture.getRealTexture(), baseMip, levelCount, baseLayer, layerCount
        );
    }

}
