package fewizz.canpipe.compat.indigo.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.compat.indigo.MeshViewExtended;
import fewizz.canpipe.compat.indigo.QuadViewExtended;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.EncodingFormat;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MeshViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.QuadViewImpl;

@Mixin(MeshViewImpl.class)
public class MeshViewImplMixin implements MeshViewExtended {

    @Shadow int limit;

    @Unique int[] canpipe_extraData;

    @Override
    public int[] canpipe_getExtraData() { return this.canpipe_extraData; }

    @Override
    public void canpipe_setExtraData(int[] data) { this.canpipe_extraData = data; }

    @Inject(
        method = "forEach(Ljava/util/function/Consumer;Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/QuadViewImpl;)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/QuadViewImpl;data:[I",
            opcode = Opcodes.PUTFIELD,
            ordinal = 0
        )
    )
    void setDataBeforeForEach(CallbackInfo ci, @Local QuadViewImpl cursor) {
        ((QuadViewExtended) cursor).canpipe_setExtraData(this.canpipe_extraData);
    }

    @Inject(
        method = "forEach(Ljava/util/function/Consumer;Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/QuadViewImpl;)V",
        at = @At("RETURN")
    )
    void setDataAfterForEach(CallbackInfo ci, @Local QuadViewImpl cursor) {
        ((QuadViewExtended) cursor).canpipe_setExtraData(null);
    }

    @Inject(
        method = "outputTo",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/System;arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V"
        )
    )
    void onCopyDataToEmitter(CallbackInfo ci, @Local(name = "index") int index, @Local MutableQuadViewImpl e) {
        System.arraycopy(
            this.canpipe_extraData,
            index / EncodingFormat.TOTAL_STRIDE * QuadViewExtended.CANPIPE_DATA_STRIDE_INTS,
            ((QuadViewExtended) e).canpipe_getExtraData(),
            ((QuadViewExtended) e).canpipe_getBaseIndex() / EncodingFormat.TOTAL_STRIDE * QuadViewExtended.CANPIPE_DATA_STRIDE_INTS,
            QuadViewExtended.CANPIPE_DATA_STRIDE_INTS
        );
    }

}
