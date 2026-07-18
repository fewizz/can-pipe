package fewizz.canpipe.b3d.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import fewizz.canpipe.b3d.RenderPipelineBuilderExtended;

// Ugly
@Mixin(RenderPipeline.Builder.class)
public abstract class RenderPipelineBuilderMixin implements RenderPipelineBuilderExtended {

    @Shadow private int activeColorTargetStateCount;
    @Unique private boolean canpipe_noColorTargetsAllowed;

    // @Shadow public abstract RenderPipeline.Builder withSampler(final String sampler);

    /*private Optional<Set<String>> canpipe_optionalSamplers = Optional.empty();

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
    }*/

    @Override
    public void canpipe_resetActiveColorTargetStateCount() { this.activeColorTargetStateCount = 0; }

    @Override public void canpipe_allowNoColorTargets() { this.canpipe_noColorTargetsAllowed = true; }

    @ModifyExpressionValue(
        method = "build",
        at = @At(  // if (this.activeColorTargetStateCount == 0) {
            value = "FIELD",
            target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;activeColorTargetStateCount:I",
            ordinal = 0
        )
    )
    int onGetColorTargetStateCount(int count) {
        if (count == 0 && this.canpipe_noColorTargetsAllowed) {
            count = 9000;
        }
        return count;
    }

}
