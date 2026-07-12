package fewizz.canpipe.b3d.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.CommandEncoderBackendExtended;
import fewizz.canpipe.b3d.CommandEncoderExtended;
import fewizz.canpipe.b3d.GpuTextureViewExtended;

@Mixin(CommandEncoder.class)
public class CommandEncoderMixin implements CommandEncoderExtended {

    @Final @Shadow private GpuDeviceBackend device;
    @Final @Shadow private CommandEncoderBackend backend;

    @Override
    public void canpipe_clearDepthTexture(GpuTexture texture, double depth, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount) {
        ((CommandEncoderBackendExtended) this.backend).canpipe_clearDepthTexture(texture, depth, baseMipLevel, levelCount, baseArrayLayer, layerCount);
    }

    @Override
    public void canpipe_clearColorTexture(GpuTexture texture, Vector4f color, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount) {
        ((CommandEncoderBackendExtended) this.backend).canpipe_clearColorTexture(texture, color, baseMipLevel, levelCount, baseArrayLayer, layerCount);
    }

    @Override
    public void canpipe_blitImage(GpuTexture srcTexture, GpuTexture dstTexture) {
        ((CommandEncoderBackendExtended) this.backend).canpipe_blitImage(srcTexture, dstTexture);
    }

    @ModifyExpressionValue(
        method = "createRenderPass(Lcom/mojang/blaze3d/systems/RenderPassDescriptor;)Lcom/mojang/blaze3d/systems/RenderPass;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/textures/GpuTexture;getDepthOrLayers()I"  // used only for checks
        )
    )
    int allowTexturesWithMultipleLayers(int value) {
        return 1;  // 1 == 1
    }

    @ModifyExpressionValue(
        method = "createRenderPass(Lcom/mojang/blaze3d/systems/RenderPassDescriptor;)Lcom/mojang/blaze3d/systems/RenderPass;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPassDescriptor$Attachment;textureView()Lcom/mojang/blaze3d/textures/GpuTextureView;"
        )
    )
    GpuTextureView checkTextureViewLayersCount(GpuTextureView textureView) {
        if (((GpuTextureViewExtended) textureView).canpipe_layerCount() > 1) {
            throw new IllegalStateException("Some attachment uses more than 1 array layers");
        }
        return textureView;
    }

}
