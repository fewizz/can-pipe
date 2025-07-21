package fewizz.canpipe.cinnabar.mixin;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.CommandEncoderExtended;
import graphics.cinnabar.core.b3d.command.CinnabarCommandEncoder;

@Mixin(CinnabarCommandEncoder.class)
public abstract class CinnabarCommandEncoderMixin implements CommandEncoderExtended {

    @Override
    public RenderPass canpipe_createRenderPass(
        Supplier<String> supplier, List<GpuTextureView> colorAttachments, @Nullable GpuTextureView depthAttachment
    ) {
        throw new RuntimeException();
    }

    @Override
    public void canpipe_clearDepthTexture(
        GpuTexture texture, double depth, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount
    ) {
        throw new RuntimeException();
    }

}
