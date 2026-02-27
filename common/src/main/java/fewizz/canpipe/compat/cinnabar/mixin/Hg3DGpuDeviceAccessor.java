package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.textures.AddressMode;

import graphics.cinnabar.api.hg.HgFramebuffer;
import graphics.cinnabar.api.hg.HgImage;
import graphics.cinnabar.api.hg.HgRenderPass;
import graphics.cinnabar.api.hg.enums.HgFormat;
import graphics.cinnabar.core.hg3d.Hg3DGpuDevice;

@Mixin(Hg3DGpuDevice.class)
public interface Hg3DGpuDeviceAccessor {

    @Accessor("canpipe_pendingTextureViewBaseLayer")
    int get_canpipe_pendingTextureViewBaseLayer();

    @Accessor("canpipe_pendingTextureViewLayerCount")
    int get_canpipe_pendingTextureViewLayerCount();

    @Accessor("canpipe_framebuffers")
    Map<Pair<List<HgImage.View>, HgImage.View>, HgFramebuffer> get_canpipe_framebuffers();

    @Accessor("canpipe_renderPasses")
    Map<Pair<List<HgFormat>, HgFormat>, HgRenderPass> get_canpipe_renderPasses();

    @Accessor("canpipe_addressModeW") AddressMode get_canpipe_addressModeW();

    @Accessor("canpipe_compareOp") CompareOp get_canpipe_compareOp();

    @Accessor("canpipe_linearMipmap") Boolean get_canpipe_linearMipmap();

}
