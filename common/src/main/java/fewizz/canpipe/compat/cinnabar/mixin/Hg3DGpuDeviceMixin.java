package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
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
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.GpuDeviceExtended;
import graphics.cinnabar.api.hg.HgDevice;
import graphics.cinnabar.api.hg.HgFramebuffer;
import graphics.cinnabar.api.hg.HgImage;
import graphics.cinnabar.api.hg.HgRenderPass;
import graphics.cinnabar.api.hg.HgSampler;
import graphics.cinnabar.api.hg.enums.HgCompareOp;
import graphics.cinnabar.api.hg.enums.HgFormat;
import graphics.cinnabar.core.hg3d.Hg3DGpuDevice;
import graphics.cinnabar.core.hg3d.Hg3DRenderPipeline;
import net.minecraft.resources.Identifier;

@Mixin(Hg3DGpuDevice.class)
public abstract class Hg3DGpuDeviceMixin implements GpuDeviceExtended {

    @Shadow @Final private HgDevice hgDevice;
    @Shadow @Final private BiFunction<Identifier, ShaderType, String> shaderSourceProvider;

    @Shadow Hg3DRenderPipeline getPipeline(RenderPipeline pipeline, BiFunction<Identifier, ShaderType, String> shaderSourceProvider) { return null; };
    @Shadow HgSampler getSampler(boolean minLinear, boolean magLinear, int addressU, int addressV, int addressW, boolean mip) { return null; }

    @Unique private Map<Pair<List<HgFormat>, HgFormat>, HgRenderPass> canpipe_renderPasses = new HashMap<>();
    @Unique private Map<Pair<List<HgImage.View>, HgImage.View>, HgFramebuffer> canpipe_framebuffers = new HashMap<>();

    @Unique private Map<HgSampler.CreateInfo, HgSampler> canpipe_samplers = new HashMap<>();

    @Unique private int canpipe_pendingTextureViewBaseLayer = -1;
    @Unique private int canpipe_pendingTextureViewLayerCount = -1;

    public HgSampler canpipe_getSampler(
        boolean minLinear, boolean magLinear, int addressU, int addressV, int addressW, boolean mip,
        @NotNull DepthTestFunction compareOp
    ) {
        HgCompareOp hgCompareOp = switch (compareOp) {
            // If specified, VkSamplerCreateInfo.compareEnable will be false, which is... fine?
            case DepthTestFunction.NO_DEPTH_TEST -> HgCompareOp.ALWAYS;
            case DepthTestFunction.EQUAL_DEPTH_TEST -> HgCompareOp.EQUAL;
            case DepthTestFunction.LEQUAL_DEPTH_TEST -> HgCompareOp.LESS_OR_EQUAL;
            case DepthTestFunction.LESS_DEPTH_TEST -> HgCompareOp.LESS;
            case DepthTestFunction.GREATER_DEPTH_TEST -> HgCompareOp.GREATER;
        };

        var createInfo = new HgSampler.CreateInfo(minLinear, magLinear, addressU, addressV, addressW, mip, hgCompareOp);
        return this.canpipe_samplers.computeIfAbsent(createInfo, k -> this.hgDevice.createSampler(createInfo));
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
            this.getPipeline(pipeline, /*shaderSource == null ? this.shaderSourceProvider : shaderSource*/ null);
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
