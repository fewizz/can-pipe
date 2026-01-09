package fewizz.canpipe.compat.cinnabar;

import java.util.List;

import org.jspecify.annotations.Nullable;

import graphics.cinnabar.api.hg.HgRenderPass;
import graphics.cinnabar.api.hg.enums.HgFormat;

public interface HgRenderPassExtended extends HgRenderPass {

    // Needed to recreate render passes, look at Hg3DRenderPassMixin

    List<HgFormat> canpipe_getColorFormats();
    @Nullable HgFormat canpipe_getDepthStencilFormat();

}
