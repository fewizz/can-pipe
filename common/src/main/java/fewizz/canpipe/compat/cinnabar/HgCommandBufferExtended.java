package fewizz.canpipe.compat.cinnabar;

import graphics.cinnabar.api.hg.HgCommandBuffer;
import graphics.cinnabar.api.hg.HgImage;

public interface HgCommandBufferExtended extends HgCommandBuffer {

    void canpipe_blitImage(
        HgImage srcImage,
        HgImage dstImage
    );

}
