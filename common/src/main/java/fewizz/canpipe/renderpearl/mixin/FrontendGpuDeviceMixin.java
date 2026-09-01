package fewizz.canpipe.renderpearl.mixin;

import java.util.OptionalDouble;

import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.frontend.FrontendGpuDevice;

import fewizz.canpipe.renderpearl.GpuDeviceBackendExtended;
import fewizz.canpipe.renderpearl.GpuDeviceExtended;
import fewizz.canpipe.renderpearl.GpuShaderModule;
import net.minecraft.resources.Identifier;

@Mixin(FrontendGpuDevice.class)
public class FrontendGpuDeviceMixin implements GpuDeviceExtended {

    @Final @Shadow  private GpuDeviceBackend backend;

    @Override
    public GpuSampler canpipe_createSampler(
        AddressMode u, AddressMode v, FilterMode min, FilterMode mag, int maxAnisotropy, OptionalDouble maxLod,
        AddressMode w, @Nullable CompareOp compareOp, boolean linearMipmap  // added
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
    public GpuShaderModule canpipe_precompileShaderModule(
        Identifier id, String shaderSource, ShaderType shaderType,
        TriConsumer<String, Identifier, String> onCompilationError
    ) {
        return ((GpuDeviceBackendExtended) this.backend).canpipe_precompilePipelineModule(id, shaderSource, shaderType, onCompilationError);
    }

    @ModifyExpressionValue(
        method = "verifyTextureCreationArgs",
        at = @At(value = "CONSTANT", args = "intValue=1", ordinal = 4)
    )
    int suppressMaxLayerCheckError(int layers) {
        return 9000;
    }

}
