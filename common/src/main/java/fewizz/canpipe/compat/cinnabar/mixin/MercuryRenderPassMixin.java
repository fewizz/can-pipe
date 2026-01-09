package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkAttachmentReference2;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.compat.cinnabar.HgRenderPassExtended;
import graphics.cinnabar.api.hg.HgRenderPass;
import graphics.cinnabar.api.hg.enums.HgFormat;
import graphics.cinnabar.core.mercury.MercuryDevice;
import graphics.cinnabar.core.mercury.MercuryRenderPass;

@Mixin(MercuryRenderPass.class)
public abstract class MercuryRenderPassMixin implements HgRenderPassExtended {

    @Shadow @Final private int colorAttachmentCount;

    private List<HgFormat> canpipe_colorFormats;
    @Nullable private HgFormat canpipe_depthStencilFormat;

    @Override
    public List<HgFormat> canpipe_getColorFormats() {
        return this.canpipe_colorFormats;
    }

    @Override
    public HgFormat canpipe_getDepthStencilFormat() {
        return this.canpipe_depthStencilFormat;
    }

    @Overwrite
    public int colorAttachmentCount() {
        return this.canpipe_colorFormats.size();  // including VK_ATTACHMENT_UNUSED
    }

    // Committing some crimes...
    // TODO: Temporary
    @ModifyConstant(
        method = "<init>",
        constant = @Constant(intValue = VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
    )
    int colorAttachmenLayoutGeneral(int original) {
        return VK10.VK_IMAGE_LAYOUT_GENERAL;
    }

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    void onInitEnd(MercuryDevice device, HgRenderPass.CreateInfo createInfo, CallbackInfo ci) {
        this.canpipe_colorFormats = createInfo.colorFormats();
        this.canpipe_depthStencilFormat = createInfo.depthStencilFormat();
    }

    @ModifyExpressionValue(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;size()I",
            ordinal = 0
        )
    )
    int onGetColorAttachmentCount(int count, @Local HgRenderPass.CreateInfo createInfo) {
        // `this.colorAttachmentCount` won't include null formats
        // they will be encoded as VK_ATTACHMENT_UNUSED
        int unusedAttachmentCount = (int) createInfo.colorFormats().stream().filter(f -> f == null).count();
        count -= unusedAttachmentCount;
        return count;
    }

    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkAttachmentReference2;calloc(ILorg/lwjgl/system/MemoryStack;)Lorg/lwjgl/vulkan/VkAttachmentReference2$Buffer;"
        ),
        index = 0
    )
    int onColorAttachmetRefsArrayCreate(int count, @Local HgRenderPass.CreateInfo createInfo) {
        // Not `this.colorAttachmentCount`, take unused attachments into account
        return createInfo.colorFormats().size();
    }

    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkSubpassDescription2$Buffer;colorAttachmentCount(I)Lorg/lwjgl/vulkan/VkSubpassDescription2$Buffer;"
        ),
        index = 0
    )
    int onSetSubpassColorAttachmentCount(int count, @Local HgRenderPass.CreateInfo createInfo) {
        // Not `this.colorAttachmentCount`, take unused attachments into account
        return createInfo.colorFormats().size();
    }

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VK12;vkCreateRenderPass2("+
                "Lorg/lwjgl/vulkan/VkDevice;"+
                "Lorg/lwjgl/vulkan/VkRenderPassCreateInfo2;"+
                "Lorg/lwjgl/vulkan/VkAllocationCallbacks;"+
                "Ljava/nio/LongBuffer;"+
            ")I"
        )
    )
    void beforeRenderPassCreation(CallbackInfo ci, @Local VkAttachmentReference2.Buffer colorReference, @Local HgRenderPass.CreateInfo createInfo) {
        // Fill left color attachment refs as unused
        for(int i = this.colorAttachmentCount; i < createInfo.colorFormats().size(); ++i) {
            ((VkAttachmentReference2.Buffer) colorReference.position(i)).sType$Default();
            colorReference.attachment(VK10.VK_ATTACHMENT_UNUSED);
         }

         colorReference.position(0);
    }

}
