package fewizz.canpipe.b3d.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.systems.RenderSystem;

import fewizz.canpipe.b3d.GpuTextureViewExtended;

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
        var device = RenderSystem.getDevice();
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
        var fboCache = ((GlDeviceAccessor) RenderSystem.getDevice()).get_canpipe_framebufferCache();
        fboCache.object2IntEntrySet().removeIf(kv -> {
            var colorsAndDepth = kv.getKey();
            var id = kv.getIntValue();
            boolean remove = false;
            for (var colorAttachment : colorsAndDepth.getKey()) {
                remove |= colorAttachment == (Object) this;
            }
            remove |= colorsAndDepth.getRight() == (Object) this;
            if (remove) {
                GlStateManager._glDeleteFramebuffers(id);
                return true;
            }
            return false;
        });
    }

}
