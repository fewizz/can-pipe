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

@Mixin(ValidationCommandEncoder.class)
public abstract class ValidationCommandEncoderMixin implements CommandEncoderExtended {
    @Shadow @Final private CommandEncoder realCommandEncoder;
    @Shadow @Final private GpuDeviceUsageValidator validator;

    @Override
    public RenderPass canpipe_createRenderPass(
        Supplier<String> supplier, GpuTextureView[] colorAttachments, @Nullable GpuTextureView depthAttachment
    ) {
        return ((CommandEncoderExtended) this.realCommandEncoder).canpipe_createRenderPass(
            supplier, colorAttachments, depthAttachment
        );
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

}
