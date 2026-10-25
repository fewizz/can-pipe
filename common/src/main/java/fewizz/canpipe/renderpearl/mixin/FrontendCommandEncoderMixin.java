package fewizz.canpipe.renderpearl.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.frontend.FrontendCommandEncoder;

import fewizz.canpipe.renderpearl.CommandEncoderBackendExtended;
import fewizz.canpipe.renderpearl.CommandEncoderExtended;

@Mixin(FrontendCommandEncoder.class)
public class FrontendCommandEncoderMixin implements CommandEncoderExtended {

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
    public void canpipe_copyTextureToTexture(GpuTexture source, GpuTexture destination, int mipLevel, int destX, int destY, int sourceX, int sourceY, int width, int height, int sourceLayer, int destinationLayer) {
        ((CommandEncoderBackendExtended) this.backend).canpipe_copyTextureToTexture(source, destination, mipLevel, destX, destY, sourceX, sourceY, width, height, sourceLayer, destinationLayer);
    }

    @Override
    public void canpipe_blitImage(GpuTexture srcTexture, GpuTexture dstTexture) {
        ((CommandEncoderBackendExtended) this.backend).canpipe_blitImage(srcTexture, dstTexture);
    }

    @ModifyExpressionValue(
        method = "createRenderPass(Lcom/mojang/renderpearl/api/commands/RenderPassDescriptor;)Lcom/mojang/renderpearl/api/commands/RenderPass;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/renderpearl/api/textures/GpuTexture;getDepthOrLayers()I"  // used only for checks
        )
    )
    int allowTexturesWithMultipleLayers(int value) {
        return 1;  // 1 == 1
    }

}
