package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import fewizz.canpipe.compat.cinnabar.HgRenderPassExtended;
import graphics.cinnabar.api.hg.HgRenderPass;
import graphics.cinnabar.api.hg.enums.HgFormat;
import graphics.cinnabar.core.hg3d.Hg3DCommandEncoder;
import graphics.cinnabar.core.hg3d.Hg3DRenderPipeline;

@Mixin(Hg3DCommandEncoder.Hg3DRenderPass.class)
public class Hg3DRenderPassMixin {

    @Shadow @Final private HgRenderPass renderPass;
    @Shadow private Hg3DRenderPipeline boundPipeline;

    // If shader expects more attachments than `this.renderPass` provides,
    // then this render pass will be created used instead
    @Unique HgRenderPass canpipe_renderPass = null;

    @ModifyArg(
        method = "setPipeline",
        at = @At(
            value = "INVOKE",
            target = "Lgraphics/cinnabar/core/hg3d/Hg3DRenderPipeline;getPipeline("+
                "Lgraphics/cinnabar/api/hg/HgRenderPass;"+
            ")Lgraphics/cinnabar/api/hg/HgGraphicsPipeline;"
        ),
        index = 0
    )
    HgRenderPass onSetPipeline(HgRenderPass renderPass) {
        int sourceSetAttachmentsCount = ((Hg3DRenderPipelineAccessor) this.boundPipeline).get_shaderSet().attachmentCount();
        int renderPassAttachmentCount = renderPass.colorAttachmentCount();

        if (
            renderPassAttachmentCount == sourceSetAttachmentsCount ||
            renderPassAttachmentCount > sourceSetAttachmentsCount  // sure?
        ) {
            // perfect, do nothing
            this.canpipe_renderPass = null;
        }
        else if (renderPassAttachmentCount < sourceSetAttachmentsCount) {
            List<HgFormat> colorFormats = new ArrayList<>(((HgRenderPassExtended) renderPass).canpipe_getColorFormats());

            // null == VK_ATTACHMENT_UNUSED
            for (int i = colorFormats.size(); i < sourceSetAttachmentsCount; ++i) {
                colorFormats.add(null);
            }

            HgFormat depthStencilFormat = ((HgRenderPassExtended) renderPass).canpipe_getDepthStencilFormat();

            var canpipe_renderPasses = ((Hg3DGpuDeviceAccessor) this.boundPipeline.device()).get_canpipe_renderPasses();

            this.canpipe_renderPass = canpipe_renderPasses.computeIfAbsent(
                Pair.of(colorFormats, depthStencilFormat),
                k -> {
                    var createInfo = new HgRenderPass.CreateInfo(colorFormats, depthStencilFormat);
                    return this.boundPipeline.device().hgDevice().createRenderPass(createInfo);
                }
            );

            renderPass = this.canpipe_renderPass;
        }

        return renderPass;
    }

}
