package fewizz.canpipe.b3d.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTextureView;

import fewizz.canpipe.b3d.GpuTextureViewExtended;
import fewizz.canpipe.b3d.RealGpuDeviceProviderService;

@Mixin(GlTextureView.class)
public class GlTextureViewMixin implements GpuTextureViewExtended {

    @Unique /*final*/ int canpipe_baseArrayLayer;
    @Unique /*final*/ int canpipe_layerCount;

    @Override
    public int canpipe_baseArrayLayer() {
        return this.canpipe_baseArrayLayer;
    }

    @Override
    public int canpipe_layerCount() {
        return this.canpipe_layerCount;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    void onInitEnd(CallbackInfo ci) {
        GlDevice device = (GlDevice) RealGpuDeviceProviderService.getRealGpuDeviceBackend();
        this.canpipe_baseArrayLayer = ((GlDeviceAccessor) device).get_canpipe_pendingTextureViewBaseLayer();
        this.canpipe_layerCount = ((GlDeviceAccessor) device).get_canpipe_pendingTextureViewLayerCount();
    }

    @Inject(
        method = "close",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlTexture;removeViews()V",
            shift = Shift.AFTER
        )
    )
    public void afterTextureRemoveViews(CallbackInfo ci) {
        GlDevice device = (GlDevice) RealGpuDeviceProviderService.getRealGpuDeviceBackend();
        var fboCache = ((GlDeviceAccessor) device).get_canpipe_framebufferCache();
        fboCache.object2IntEntrySet().removeIf(kv -> {
            var textureViews = kv.getKey();
            var fboID = kv.getIntValue();
            for (var textureView : textureViews) {
                if (textureView == (Object) this) {
                    GlStateManager._glDeleteFramebuffers(fboID);
                    return true;
                }
            }
            return false;
        });
    }

}
