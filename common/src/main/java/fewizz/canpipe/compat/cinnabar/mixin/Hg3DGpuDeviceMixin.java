package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Supplier;

import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.GpuDeviceBackendExtended;
import fewizz.canpipe.b3d.GpuSamplerExteneded;
import graphics.cinnabar.api.hg.HgDevice;
import graphics.cinnabar.api.hg.HgFramebuffer;
import graphics.cinnabar.api.hg.HgImage;
import graphics.cinnabar.api.hg.HgRenderPass;
import graphics.cinnabar.api.hg.enums.HgFormat;
import graphics.cinnabar.core.hg3d.Hg3DGpuDevice;
import graphics.cinnabar.core.hg3d.Hg3DRenderPipeline;
import net.minecraft.resources.Identifier;

@Mixin(Hg3DGpuDevice.class)
public abstract class Hg3DGpuDeviceMixin implements GpuDeviceBackendExtended {

    @Shadow @Final private HgDevice hgDevice;
    @Shadow @Final private ShaderSource shaderSourceProvider;

    @Shadow Hg3DRenderPipeline getPipeline(RenderPipeline pipeline, ShaderSource shaderSourceProvider) { return null; };

    @Unique private Map<Pair<List<HgFormat>, HgFormat>, HgRenderPass> canpipe_renderPasses = new HashMap<>();
    @Unique private Map<Pair<List<HgImage.View>, HgImage.View>, HgFramebuffer> canpipe_framebuffers = new HashMap<>();

    @Unique private int canpipe_pendingTextureViewBaseLayer = -1;
    @Unique private int canpipe_pendingTextureViewLayerCount = -1;

    @Unique private AddressMode canpipe_addressModeW = null;
    @Unique private DepthTestFunction canpipe_compareOp = null;
    @Unique private Boolean canpipe_linearMipmap = null;

    @Override
    public GpuSamplerExteneded canpipe_createSampler(
        AddressMode u, AddressMode v,
        FilterMode min, FilterMode mag,
        int maxAnisotropy, OptionalDouble maxLod,
        AddressMode w, @Nullable DepthTestFunction compareOp, boolean linearMipmap  // added
    ) {
        try {
            this.canpipe_addressModeW = w;
            this.canpipe_compareOp = compareOp;
            this.canpipe_linearMipmap = linearMipmap;
            return (GpuSamplerExteneded) this.createSampler(u, v, min, mag, 0, maxLod);
        } finally {
            this.canpipe_addressModeW = null;
            this.canpipe_compareOp = null;
            this.canpipe_linearMipmap = null;
        }
    }

    @Override
    public void canpipe_precompilePipelineShaderModules(
        RenderPipeline pipeline,
        ShaderSource shaderSource,
        TriConsumer<String, Identifier, String> onCompilationError
    ) {
        // not `this.precompilePipeline()`, I don't want to create a graphics pipeline
        try {
            ((MercuryDeviceAccessor) this.hgDevice).set_canpipe_onCompilationError((error, shaderType, src) -> {
                onCompilationError.accept(  // convert shader type to shader location
                    error,
                    shaderType == ShaderType.VERTEX ? pipeline.getVertexShader() : pipeline.getFragmentShader(),
                    src
                );
            });
            this.getPipeline(pipeline, shaderSource == null ? this.shaderSourceProvider : shaderSource);
        } finally {
            ((MercuryDeviceAccessor) this.hgDevice).set_canpipe_onCompilationError(null);
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
        } finally {
            this.canpipe_pendingTextureViewBaseLayer = -1;
            this.canpipe_pendingTextureViewLayerCount = -1;
        }
    }

    @Inject(
        method = "clearPipelineCache",
        at = @At("TAIL")
    )
    public void clearShaderSourceCacheOnPipelineCacheClear(CallbackInfo ci) {
        Hg3DRenderPipelineAccessor.getShaderSourceCache().clear();
    }

    @ModifyArg(
        method = "Lgraphics/cinnabar/core/hg3d/Hg3DGpuDevice;createTexture(Ljava/util/function/Supplier;ILcom/mojang/blaze3d/textures/TextureFormat;IIII)Lcom/mojang/blaze3d/textures/GpuTexture;",
        at = @At(
            value = "INVOKE",
            target = "Lgraphics/cinnabar/core/hg3d/Hg3DGpuTexture;<init>(Lgraphics/cinnabar/core/hg3d/Hg3DGpuDevice;ILjava/lang/String;Lcom/mojang/blaze3d/textures/TextureFormat;IIII)V"
        ),
        index = 2  // label
    )
    public String fixTextureLabelSupplier(String original, @Local Supplier<String> label) {
        return label.get();
    }

    @ModifyArg(
        method = "Lgraphics/cinnabar/core/hg3d/Hg3DGpuDevice;createTexture(Ljava/lang/String;ILcom/mojang/blaze3d/textures/TextureFormat;IIII)Lcom/mojang/blaze3d/textures/GpuTexture;",
        at = @At(
            value = "INVOKE",
            target = "Lgraphics/cinnabar/core/hg3d/Hg3DGpuTexture;<init>(Lgraphics/cinnabar/core/hg3d/Hg3DGpuDevice;ILjava/lang/String;Lcom/mojang/blaze3d/textures/TextureFormat;IIII)V"
        ),
        index = 2  // label
    )
    public String fixTextureLabelString(String original, @Local(ordinal = 0) String label) {
        return label;
    }

}
