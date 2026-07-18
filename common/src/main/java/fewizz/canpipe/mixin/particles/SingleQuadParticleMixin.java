package fewizz.canpipe.mixin.particles;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.material.Material;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.mixininterface.QuadParticleRenderStateExtended;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.ParticleType;

@Mixin(SingleQuadParticle.class)
public class SingleQuadParticleMixin {

    @Inject(
        method = "extractRotatedQuad(Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;Lorg/joml/Quaternionf;FFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;add(Lnet/minecraft/client/particle/SingleQuadParticle$Layer;FFFFFFFFFFFFII)V"
        )
    )
    void beforeAddingToRenderState(CallbackInfo ci, @Local(argsOnly = true) QuadParticleRenderState particleTypeRenderState) {
        ParticleType<?> type = ((ParticleAccessor) this).canpipe_getParticleType();
        MaterialMap materialMap = MaterialMaps.getForParticle(type);
        Material material = materialMap != null ? materialMap.defaultMaterial() : null;
        ((QuadParticleRenderStateExtended) particleTypeRenderState).canpipe_setPendingMaterial(material);
    }

}
