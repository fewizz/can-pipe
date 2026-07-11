package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.state.GameRenderState;

@Mixin(GameRenderState.class)
public class GameRenderStateMixin {

    @ModifyReturnValue(
        method = "useShaderTransparency",
        at = @At("RETURN")
    )
    private boolean useShaderTransparency(boolean original) {
        Pipeline p = Pipelines.getCurrent();
        return original || (p != null && p.fabulousTargets != null);
    }

}
