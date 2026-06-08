package fewizz.canpipe.compat.indigo.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import fewizz.canpipe.compat.indigo.MeshViewExtended;
import fewizz.canpipe.compat.indigo.MutableQuadViewExtended;
import fewizz.canpipe.compat.indigo.QuadViewExtended;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.EncodingFormat;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableMeshImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;

@Mixin(MutableMeshImpl.class)
public class MutableMeshImplMixin extends MeshViewImplMixin {

    @Shadow @Final private MutableQuadViewImpl emitter;

    @Inject(
        method = "<init>",
        at = @At(
            value = "FIELD",
            opcode = Opcodes.PUTFIELD,
            target = "data:[I",
            ordinal = 0
        )
    )
    void onInit(CallbackInfo ci) {
        this.canpipe_extraData = new int[8 * QuadViewExtended.CANPIPE_DATA_STRIDE_INTS];
    }

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;clear()Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;"
        )
    )
    void onInitBedoreEmitterClear(CallbackInfo ci) {
        ((MutableQuadViewExtended) this.emitter).canpipe_setExtraData(this.canpipe_extraData);
    }

    @Inject(
        method = "ensureCapacity",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/System;arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V"
        )
    )
    void onEnsureCapacity(CallbackInfo ci) {
        final int[] bigger = new int[this.canpipe_extraData.length * 2];
        System.arraycopy(this.canpipe_extraData, 0, bigger, 0, this. limit / EncodingFormat.TOTAL_STRIDE * QuadViewExtended.CANPIPE_DATA_STRIDE_INTS);
        this.canpipe_extraData = bigger;
        ((MutableQuadViewExtended) this.emitter).canpipe_setExtraData(bigger);
    }

    @Inject(method = "forEachMutable", at = @At("RETURN"))
    void onForEachMutable(CallbackInfo ci) {
        ((MutableQuadViewExtended) this.emitter).canpipe_setExtraData(this.canpipe_extraData);
    }

    @ModifyReturnValue(method = "immutableCopy", at = @At("RETURN"))
    Mesh onImmutableCopy(Mesh mesh) {
        ((MeshViewExtended) mesh).canpipe_setExtraData(this.canpipe_extraData);
        return mesh;
    }

}
