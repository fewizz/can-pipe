package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;

import graphics.cinnabar.api.hg.HgFramebuffer;
import graphics.cinnabar.api.hg.HgImage;
import graphics.cinnabar.api.hg.HgRenderPass;
import graphics.cinnabar.api.hg.HgSampler;
import graphics.cinnabar.api.hg.enums.HgFormat;
import graphics.cinnabar.core.hg3d.Hg3DGpuDevice;

@Mixin(Hg3DGpuDevice.class)
public interface Hg3DGpuDeviceAccessor {

    @Accessor("canpipe_pendingColorAttachments")
    void set_canpipe_pendingColorAttachments(GpuTextureView[] colors);

    @Accessor("canpipe_pendingColorAttachments")
    GpuTextureView[] get_canpipe_pendingColorAttachments();

    @Accessor("canpipe_pendingTextureViewBaseLayer")
    int get_canpipe_pendingTextureViewBaseLayer();

    @Accessor("canpipe_pendingTextureViewLayerCount")
    int get_canpipe_pendingTextureViewLayerCount();

    @Accessor("canpipe_framebuffers")
    Map<Pair<List<HgImage.View>, HgImage.View>, HgFramebuffer> get_canpipe_framebuffers();

    @Invoker("canpipe_getRenderPass")
    HgRenderPass canpipe_getRenderPass(List<HgFormat> colorFormats, @Nullable HgFormat depthStencilFormat);

    @Invoker("canpipe_getSampler")
    HgSampler canpipe_getSampler(
        boolean minLinear, boolean magLinear, int addressU, int addressV, int addressW, boolean mip,
        FilterMode mipFilter, DepthTestFunction compareOp
    );
    
}
