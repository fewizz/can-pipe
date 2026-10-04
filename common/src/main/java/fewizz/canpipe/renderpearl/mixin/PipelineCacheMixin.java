package fewizz.canpipe.renderpearl.mixin;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import fewizz.canpipe.renderpearl.PipelineCacheExtended;

@Mixin(PipelineCache.class)
public class PipelineCacheMixin implements PipelineCacheExtended {

    @Unique private final Map<CompiledRenderPipeline, RenderPipeline> canpipe_pipelineByCompiled = new HashMap<>();

    @Override
    public RenderPipeline canpipe_getPipeline(CompiledRenderPipeline compiled) {
        return this.canpipe_pipelineByCompiled.get(compiled);
    }

    @Inject(method = "insert", at = @At("RETURN"))
    public void onInsert(CallbackInfo ci, @Local RenderPipeline pipeline, @Local CompiledRenderPipeline compiled) {
        if (this.canpipe_pipelineByCompiled.get(compiled) != null) {
            throw new RuntimeException("Pipeline \""+pipeline.getLocation()+"\" is already cached");
        }
        this.canpipe_pipelineByCompiled.put(compiled, pipeline);
    }

    @Inject(
        method = "get",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            shift = Shift.AFTER
        )
    )
    public void onGet(
        CallbackInfoReturnable<CompiledRenderPipeline> ci,
        @Local(name = "pipeline") RenderPipeline pipeline,
        @Local(name = "newPipeline") CompiledRenderPipeline newPipeline
    ) {
        RenderPipeline prevRenderPipeline = this.canpipe_pipelineByCompiled.get(newPipeline);
        if (prevRenderPipeline != null) {
            throw new RuntimeException("Pipeline \""+pipeline.getLocation()+"\" is already cached");
        }
        this.canpipe_pipelineByCompiled.put(newPipeline, pipeline);
    }

    @Inject(method = "clear", at = @At("RETURN"))
    public void onClear(CallbackInfo ci) {
        this.canpipe_pipelineByCompiled.clear();
    }

}
