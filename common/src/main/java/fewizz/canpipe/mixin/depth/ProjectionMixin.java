package fewizz.canpipe.mixin.depth;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.ProjectionType;

import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.Projection;

@Mixin(Projection.class)
public class ProjectionMixin {

    @Shadow private ProjectionType projectionType = ProjectionType.PERSPECTIVE;
    @Shadow @Final private Matrix4f matrix;
    @Shadow private float zNear;
	@Shadow private float zFar;

    @ModifyExpressionValue(
        method = "getMatrix", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/Projection;zNear:F")
    )
    float replaceNear(float value) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null && !p.awareOfDepthRangeChanges) {
            value = this.zFar;
        }
        return value;
    }

    @ModifyExpressionValue(
        method = "getMatrix", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/Projection;zFar:F")
    )
    float replaceFar(float value) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null && !p.awareOfDepthRangeChanges) {
            value = this.zNear;
        }
        return value;
    }

    @ModifyExpressionValue(
        method = "getMatrix",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/DeviceInfo;isZZeroToOne()Z")
    )
    boolean replaceIsZZeroToOne(boolean value) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null && !p.awareOfDepthRangeChanges) {
            value = false;
        }
        return value;
    }

}
