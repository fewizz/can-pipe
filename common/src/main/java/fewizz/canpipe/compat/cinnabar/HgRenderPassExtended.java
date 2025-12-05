package fewizz.canpipe.compat.cinnabar;

import java.util.List;

import blue.endless.jankson.annotation.Nullable;
import graphics.cinnabar.api.hg.HgRenderPass;
import graphics.cinnabar.api.hg.enums.HgFormat;

public interface HgRenderPassExtended extends HgRenderPass {

    // Needed to recreate render passes, look at Hg3DRenderPassMixin

    List<HgFormat> getColorFormats();
    @Nullable HgFormat getDepthStencilFormat();

}
