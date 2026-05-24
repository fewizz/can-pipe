package fewizz.canpipe.b3d.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.pipeline.RenderPipeline;

@Mixin(value = RenderPipeline.class, priority = 1001)
public interface RenderPipelineAccessor {

    @Accessor("canpipe_optionalSamplers")
    public void canpipe_setOptionalSamplers(Set<String> samplers);

}
