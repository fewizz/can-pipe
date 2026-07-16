package fewizz.canpipe.b3d.mixin;

import java.util.OptionalDouble;

import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.glsl.IntermediaryShaderModule;

import fewizz.canpipe.b3d.GpuDeviceBackendExtended;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;

@Mixin(VulkanDevice.class)
public abstract class VkDeviceMixin implements GpuDeviceBackendExtended {

    @Shadow abstract IntermediaryShaderModule getOrCompileShader(final Identifier id, final ShaderType type, final ShaderDefines defines, final ShaderSource shaderSource);

    @Unique private TriConsumer<String, Identifier, String> canpipe_onCompilationError = null;
    @Unique private String canpipe_compilationLog = null;

    @Unique private int canpipe_pendingTextureViewBaseLayer = -1;
    @Unique private int canpipe_pendingTextureViewLayerCount = -1;

    @Unique private AddressMode canpipe_addressModeW = null;
    @Unique private CompareOp canpipe_compareOp = null;
    @Unique private Boolean canpipe_linearMipmap = null;

    @Override
    public GpuSampler canpipe_createSampler(
        AddressMode u, AddressMode v,
        FilterMode min, FilterMode mag,
        int maxAnisotropy, OptionalDouble maxLod,
        AddressMode w, @Nullable CompareOp compareOp, boolean linearMipmap
    ) {
        try {
            this.canpipe_addressModeW = w;
            this.canpipe_compareOp = compareOp;
            this.canpipe_linearMipmap = linearMipmap;
            return this.createSampler(u, v, min, mag, maxAnisotropy, maxLod);
        } finally {
            this.canpipe_addressModeW = null;
            this.canpipe_compareOp = null;
            this.canpipe_linearMipmap = null;
        }
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
        }
        finally {
            this.canpipe_pendingTextureViewBaseLayer = -1;
            this.canpipe_pendingTextureViewLayerCount = -1;
        }
    }

    @Override
    public void canpipe_precompilePipelineModule(
        Identifier id,
        String shaderSource,
        ShaderType shaderType,
        TriConsumer<String, Identifier, String> onCompilationError
    ) {
        try {
            this.canpipe_onCompilationError = onCompilationError;
            this.getOrCompileShader(id, shaderType, ShaderDefines.EMPTY, (_id, _type) -> shaderSource);
        } finally {
            this.canpipe_onCompilationError = null;
            this.canpipe_compilationLog = null;
        }
    }

}
