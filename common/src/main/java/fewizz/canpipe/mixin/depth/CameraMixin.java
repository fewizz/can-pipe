package fewizz.canpipe.mixin.depth;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Camera;

@Mixin(Camera.class)
public class CameraMixin {

    @ModifyExpressionValue(
        method = "createProjectionMatrixForCulling",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/DeviceInfo;isZZeroToOne()Z")
    )
    boolean canpipe_replaceIsZZeroToZone(boolean value) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null && !p.awareOfDepthRangeChanges) {
            value = false;
        }
        return value;
    }

}
