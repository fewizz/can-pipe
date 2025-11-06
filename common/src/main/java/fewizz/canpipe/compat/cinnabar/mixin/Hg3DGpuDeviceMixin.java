package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.GpuDeviceExtended;
import graphics.cinnabar.core.hg3d.Hg3DGpuDevice;
import graphics.cinnabar.core.hg3d.Hg3DRenderPipeline;
import net.minecraft.resources.ResourceLocation;

@Mixin(Hg3DGpuDevice.class)
public abstract class Hg3DGpuDeviceMixin implements GpuDeviceExtended {

    @Shadow @Final private BiFunction<ResourceLocation, ShaderType, String> shaderSourceProvider;
    @Shadow Hg3DRenderPipeline getPipeline(RenderPipeline pipeline, BiFunction<ResourceLocation, ShaderType, String> shaderSourceProvider) { return null; };

    @Unique private int canpipe_pendingTextureViewBaseLayer = -1;
    @Unique private int canpipe_pendingTextureViewLayerCount = -1;
    @Unique GpuTextureView[] canpipe_pendingColorAttachments = null;

    @Override
    public CompiledRenderPipeline canpipe_precompilePipeline(
        RenderPipeline pipeline,
        BiFunction<ResourceLocation, ShaderType, String> shaderSource,
        TriConsumer<String, ResourceLocation, String> onCompilationError
    ) {
        // not `this.precompilePipeline()`, I don't want to create a graphics pipeline
        return this.getPipeline(pipeline, shaderSource == null ? this.shaderSourceProvider : shaderSource);
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

    /*@Overwrite
    public HgRenderPass getRenderPass(HgFormat colorFormat, @Nullable HgFormat depthStencilFormat) {
        var colorFormats = new ArrayList<HgFormat>();
        if (this.canpipe_pendingColorAttachments != null) {
            for (var a : this.canpipe_pendingColorAttachments) {
                colorFormats.add(((Hg3DGpuTextureView)a).imageView().format());
            }
        }
        else {
            colorFormats.add(colorFormat);
        }
        return _getRenderPass(colorFormats, depthStencilFormat);
    }*/
    
}
