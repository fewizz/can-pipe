package fewizz.canpipe.b3d.mixin;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import fewizz.canpipe.b3d.RenderPipelineBuilderExtended;

// Ugly
@Mixin(RenderPipeline.Builder.class)
public abstract class RenderPipelineBuilderMixin implements RenderPipelineBuilderExtended {

    @Shadow public abstract RenderPipeline.Builder withSampler(final String sampler);

    private Optional<Set<String>> canpipe_optionalSamplers = Optional.empty();

    @Override
    public RenderPipeline.Builder canpipe_withOptionalSampler(String sampler) {
        var self = this.withSampler(sampler);
        if (this.canpipe_optionalSamplers.isEmpty()) {
            this.canpipe_optionalSamplers = Optional.of(new HashSet<String>());
        }
        this.canpipe_optionalSamplers.get().add(sampler);
        return self;
    }

    @ModifyReturnValue(method = "build", at = @At("RETURN"))
    RenderPipeline afterBuild(RenderPipeline pipeline) {
        Set<String> optionalSamplers = this.canpipe_optionalSamplers.orElse(Set.of());
        ((RenderPipelineAccessor) pipeline).canpipe_setOptionalSamplers(Set.copyOf(optionalSamplers));
        return pipeline;
    }

}
