package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @ModifyExpressionValue(
        method = "makeParticle",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleProvider;createParticle("+
                "Lnet/minecraft/core/particles/ParticleOptions;"+
                "Lnet/minecraft/client/multiplayer/ClientLevel;"+
                "DDDDDD"+ // :O
                "Lnet/minecraft/util/RandomSource;"+
            ")Lnet/minecraft/client/particle/Particle;"
        )
    )
    Particle onParticleCreated(Particle particle, @Local ParticleOptions options) {
        ((ParticleAccessor) particle).canpipe_setParticleType(options.getType());
        return particle;
    }

}
