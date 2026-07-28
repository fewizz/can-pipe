package fewizz.canpipe.compat.indigo.mixin;

import fewizz.canpipe.compat.indigo.QuadViewExtended;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {"net.fabricmc.fabric.impl.client.indigo.renderer.render.ExtendedItemFeatureRenderer$1"})
public class ExtendedItemFeatureRendererEmitterMixin {

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
