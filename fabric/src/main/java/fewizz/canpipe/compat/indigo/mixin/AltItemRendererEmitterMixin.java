package fewizz.canpipe.compat.indigo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fewizz.canpipe.compat.indigo.QuadViewExtended;

@Mixin(targets = {"net.fabricmc.fabric.impl.client.indigo.renderer.render.AltItemRenderer$1"})
public class AltItemRendererEmitterMixin {

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "clear()Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;"
        )
    )
    void onInit(CallbackInfo ci) {
        ((QuadViewExtended) this).canpipe_setExtraData(new int[QuadViewImplMixin.CANPIPE_DATA_STRIDE_INTS]);
    }

}
