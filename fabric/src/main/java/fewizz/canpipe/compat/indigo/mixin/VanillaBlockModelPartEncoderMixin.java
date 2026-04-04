package fewizz.canpipe.compat.indigo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.compat.indigo.MutableQuadViewExtended;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.impl.client.renderer.VanillaBlockModelPartEncoder;
import net.minecraft.client.resources.model.geometry.BakedQuad;

@Mixin(VanillaBlockModelPartEncoder.class)
public class VanillaBlockModelPartEncoderMixin {

    @Inject(
        method = "emitQuads",
        at = @At(
            value = "INVOKE",
            target = "Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadEmitter;emit()Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadEmitter;"
        )
    )
    private static void beforeEmit(CallbackInfo ci, @Local QuadEmitter emitter, @Local BakedQuad quad) {
        ((MutableQuadViewExtended) emitter).canpipe_setSprite(quad.materialInfo().sprite());
    }

}
