package fewizz.canpipe.b3d.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.RenderSystem;

import fewizz.canpipe.b3d.GpuTextureViewExtended;

@Mixin(GlTextureView.class)
public class GlTextureViewMixin implements GpuTextureViewExtended {

    @Unique /*final*/ int canpipe_baseArrayLayer;
    @Unique /*final*/ int canpipe_layerCount;

    @Override public int canpipe_baseArrayLayer() { return this.canpipe_baseArrayLayer; }
    @Override public int canpipe_layerCount() { return this.canpipe_layerCount; }

    @Inject(method = "<init>", at = @At("TAIL"))
    void onInitEnd(CallbackInfo ci) {
        GpuDeviceBackend device = ((GpuDeviceAccessor) RenderSystem.getDevice()).canpipe_getBackend();
        this.canpipe_baseArrayLayer = ((GlDeviceAccessor) device).get_canpipe_pendingTextureViewBaseLayer();
        this.canpipe_layerCount = ((GlDeviceAccessor) device).get_canpipe_pendingTextureViewLayerCount();
    }

}
