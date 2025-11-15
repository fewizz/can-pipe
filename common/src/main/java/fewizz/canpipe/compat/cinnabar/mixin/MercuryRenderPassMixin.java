package fewizz.canpipe.compat.cinnabar.mixin;

import org.lwjgl.vulkan.VK10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import graphics.cinnabar.core.mercury.MercuryRenderPass;

@Mixin(MercuryRenderPass.class)
public class MercuryRenderPassMixin {

    // Committing some crimes...
    // TODO: Temporary
    @ModifyConstant(
        method = "<init>",
        constant = @Constant(intValue = VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
    )
    int colorAttachmenLayoutGeneral(int original) {
        return VK10.VK_IMAGE_LAYOUT_GENERAL;
    }

}
