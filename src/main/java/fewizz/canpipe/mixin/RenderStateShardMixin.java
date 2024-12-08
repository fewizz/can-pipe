package fewizz.canpipe.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.Pipelines;
import fewizz.canpipe.pipeline.Pipeline;
import net.minecraft.client.renderer.RenderStateShard;

@Mixin(RenderStateShard.class)
public class RenderStateShardMixin {

    @ModifyVariable(
        method = "<init>",
        at = @At(value = "HEAD"),
        ordinal = 0
    )
    private static Runnable replaceSetup(Runnable setup, @Local String name) {
        Set<String> solidTargets = Set.of(
            "solid", "cutout", "cutout_mipped", "entity_solid",
            "entity_solid_z_offset_forward", "entity_cutout", "entity_cutout_no_cull",
            "entity_cutout_no_cull_z_offset", "entity_smooth_cutout"
        );

        if (solidTargets.contains(name)) {
            return () -> {
                setup.run();
                Pipeline p = Pipelines.getCurrent();
                if (p != null) {
                    p.solidTerrainFramebuffer.bindWrite(false);
                }
            };
        };
        return setup;
    }

}