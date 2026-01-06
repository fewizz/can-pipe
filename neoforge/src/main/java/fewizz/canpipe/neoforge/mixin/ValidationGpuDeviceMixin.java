package fewizz.canpipe.neoforge.mixin;

import java.util.OptionalDouble;

import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.GpuDeviceExtended;
import fewizz.canpipe.b3d.GpuSamplerExteneded;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuDevice;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuTexture;

@Mixin(ValidationGpuDevice.class)
public abstract class ValidationGpuDeviceMixin implements GpuDeviceExtended {

    @Shadow @Final protected GpuDevice realDevice;

    public GpuSamplerExteneded canpipe_createSampler(
        AddressMode u, AddressMode v, FilterMode min, FilterMode mag, int maxAnisotropy, OptionalDouble maxLod,
        AddressMode w, DepthTestFunction compareOp, boolean linearMipmap  // added
    ) {
        return ((GpuDeviceExtended) this.realDevice).canpipe_createSampler(
            u, v, min, mag, maxAnisotropy, maxLod, w, compareOp, linearMipmap
        );
    }

    @Override
    public void canpipe_precompilePipelineShaderModules(
        RenderPipeline pipeline,
        ShaderSource shaderSource,
        TriConsumer<String, Identifier, String> onCompilationError
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
