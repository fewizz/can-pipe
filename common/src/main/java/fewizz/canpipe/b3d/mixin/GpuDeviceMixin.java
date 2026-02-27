package fewizz.canpipe.b3d.mixin;

import java.util.OptionalDouble;

import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.GpuDeviceBackendExtended;
import fewizz.canpipe.b3d.GpuDeviceExtended;
import net.minecraft.resources.Identifier;

@Mixin(GpuDevice.class)
public class GpuDeviceMixin implements GpuDeviceExtended {

    @Final private GpuDeviceBackend backend;

    @Override
    public GpuSampler canpipe_createSampler(
        AddressMode u, AddressMode v, FilterMode min, FilterMode mag, int maxAnisotropy, OptionalDouble maxLod,
        AddressMode w, @Nullable DepthTestFunction compareOp, boolean linearMipmap  // added
    ) {
        return ((GpuDeviceBackendExtended) this.backend).canpipe_createSampler(
            u, v, min, mag, maxAnisotropy, maxLod,
            w, compareOp, linearMipmap
        );
    }

    @Override
    public GpuTextureView canpipe_createTextureView(
        GpuTexture gpuTexture, int baseMip, int levelCount,
        int baseLayer, int layerCount // added
    ) {
        return ((GpuDeviceBackendExtended) this.backend).canpipe_createTextureView(
            gpuTexture, baseMip, levelCount, baseLayer, layerCount
        );
    }

    @Override
    public void canpipe_precompilePipelineShaderModules(
        RenderPipeline pipeline,
        ShaderSource shaderSource,
        TriConsumer<String, Identifier, String> onCompilationError
    ) {
        ((GpuDeviceBackendExtended) this.backend).canpipe_precompilePipelineShaderModules(
            pipeline, shaderSource, onCompilationError
        );
    }

    @ModifyExpressionValue(
        method = "verifyTextureCreationArgs",
        at = @At(value = "CONSTANT", args = "intValue=1", ordinal = 4)
    )
    int suppressMaxLayerCheckError(int layers) {
        return 9000;
    }

}
