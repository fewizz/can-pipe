package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.ShaderManager;

@Mixin(ShaderManager.class)
public class ShaderManagerMixin {

    @Inject(
        method = "apply(Lnet/minecraft/client/renderer/ShaderManager$Configs;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/GpuDevice;clearPipelineCache()V"
        )
    )
    void resetPipelineOnApply(CallbackInfo ci) {
        /*
        Not needed if `Pipelines` resource reload listener's "apply" stage is executed on same tick as `ModelManager`'s "apply" stage
        But some mods (or, for example, fabric-model-loading-api-v1) can defer "apply" stage of resource listeners ordered before `Pipelines`
        */
        if (Pipelines.getCurrent() != null) {
            Pipelines.loadAndSetPipeline(null, null, false /* don't override saved selected pipeline */);
        }
    }

}
