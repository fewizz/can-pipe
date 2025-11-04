package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.GpuDeviceExtended;
import graphics.cinnabar.api.hg.HgRenderPass;
import graphics.cinnabar.api.hg.enums.HgFormat;
import graphics.cinnabar.core.hg3d.Hg3DGpuDevice;
import graphics.cinnabar.core.hg3d.Hg3DGpuTextureView;
import net.minecraft.resources.ResourceLocation;

@Mixin(Hg3DGpuDevice.class)
public abstract class Hg3DGpuDeviceMixin implements GpuDeviceExtended {

    @Shadow public abstract HgRenderPass _getRenderPass(List<HgFormat> colorFormats, @Nullable HgFormat depthStencilFormat);

    @Unique private int canpipe_pendingTextureViewBaseLayer = -1;
    @Unique private int canpipe_pendingTextureViewLayerCount = -1;
    @Unique GpuTextureView[] canpipe_pendingColorAttachments = null;

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

    @Overwrite
    public HgRenderPass getRenderPass(HgFormat colorFormat, @Nullable HgFormat depthStencilFormat) {
        var colorFormats = new ArrayList<HgFormat>();
        if (this.canpipe_pendingColorAttachments != null) {
            System.out.println("Overriding color fotmats: "+this.canpipe_pendingColorAttachments.length);
            for (var a : this.canpipe_pendingColorAttachments) {
                colorFormats.add(((Hg3DGpuTextureView)a).imageView().format());
            }
        }
        else {
            colorFormats.add(colorFormat);
        }
        return _getRenderPass(colorFormats, depthStencilFormat);
    }
    
}
