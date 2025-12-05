package fewizz.canpipe.neoforge.mixin;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.CommandEncoderExtended;
import net.neoforged.neoforge.client.blaze3d.validation.GpuDeviceUsageValidator;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationCommandEncoder;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuTexture;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuTextureView;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationRenderPass;

@Mixin(ValidationCommandEncoder.class)
public abstract class ValidationCommandEncoderMixin implements CommandEncoderExtended {

    @Shadow @Final private CommandEncoder realCommandEncoder;
    @Shadow @Final private GpuDeviceUsageValidator validator;

    @Shadow protected ValidationRenderPass wrapRenderPass(RenderPass renderPass, GpuDeviceUsageValidator validator) { return null; }

    @Override
    public RenderPass canpipe_createRenderPass(
        Supplier<String> supplier, GpuTextureView[] colorAttachments, @Nullable GpuTextureView depthAttachment
    ) {
        colorAttachments = colorAttachments.clone();

        for (int i = 0; i < colorAttachments.length; ++i) {
            if (colorAttachments[i] instanceof ValidationGpuTextureView validationTextureView) {
                colorAttachments[i] = validationTextureView.getRealTextureView();
            }
        }

        var renderPass = ((CommandEncoderExtended) this.realCommandEncoder).canpipe_createRenderPass(
            supplier, colorAttachments, depthAttachment
        );

        return wrapRenderPass(renderPass, validator);
    }

    @Override
    public void canpipe_clearDepthTexture(
        GpuTexture texture, double depth, int baseMipLevel, int levelCount,
        int baseArrayLayer, int layerCount
    ) {
        if (!(texture instanceof ValidationGpuTexture validationTexture)) {
            throw new IllegalArgumentException();
        }
        ((CommandEncoderExtended) this.realCommandEncoder).canpipe_clearDepthTexture(
            validationTexture.getRealTexture(), depth, baseMipLevel, levelCount, baseArrayLayer, layerCount
        );
    }

    @Override
    public void canpipe_clearColorTexture(
        GpuTexture texture, int color, int baseMipLevel, int levelCount,
        int baseArrayLayer, int layerCount
    ) {
        if (!(texture instanceof ValidationGpuTexture validationTexture)) {
            throw new IllegalArgumentException();
        }
        ((CommandEncoderExtended) this.realCommandEncoder).canpipe_clearColorTexture(
            validationTexture.getRealTexture(), color, baseMipLevel, levelCount, baseArrayLayer, layerCount
        );
    }

    @Override
    public void canpipe_blitImage(GpuTexture srcTexture, GpuTexture dstTexture) {
        if (!(srcTexture instanceof ValidationGpuTexture srcValidationTexture)) {
            throw new IllegalArgumentException();
        }
        if (!(dstTexture instanceof ValidationGpuTexture dstValidationTexture)) {
            throw new IllegalArgumentException();
        }
        ((CommandEncoderExtended) this.realCommandEncoder).canpipe_blitImage(
            srcValidationTexture.getRealTexture(), dstValidationTexture.getRealTexture()
        );
    }

}
