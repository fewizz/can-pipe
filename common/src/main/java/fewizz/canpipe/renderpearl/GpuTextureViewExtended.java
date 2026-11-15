package fewizz.canpipe.renderpearl;

import com.mojang.renderpearl.api.textures.GpuTextureView;

public interface GpuTextureViewExtended extends GpuTextureView {

    int canpipe_baseArrayLayer();
    int canpipe_layerCount();

}
