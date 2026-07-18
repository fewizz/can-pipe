package fewizz.canpipe.mixin.particles;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleType;

@Mixin(value = Particle.class, priority = 1001)
public interface ParticleAccessor {

    @Accessor("canpipe_particleType")
    ParticleType<?> canpipe_getParticleType();

    @Accessor("canpipe_particleType")
    void canpipe_setParticleType(ParticleType<?> particleType);

}
