package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleType;

@Mixin(Particle.class)
public class ParticleMixin {

    ParticleType<?> canpipe_particleType;

}
