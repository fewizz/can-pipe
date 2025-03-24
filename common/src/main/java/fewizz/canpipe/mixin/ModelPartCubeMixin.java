package fewizz.canpipe.mixin;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.mixinclass.NormalAndTangent;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.model.geom.ModelPart;


@Mixin(ModelPart.Cube.class)
public class ModelPartCubeMixin {

    @Inject(
        method = "compile",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFFIFFIIFFF)V",
            shift = Shift.AFTER
        )
    )
    void addTangent(
        CallbackInfo ci,
        @Local(argsOnly = true) VertexConsumer buffer,
        @Local(argsOnly = true) PoseStack.Pose pose,
        @Local(ordinal = 0) Vector3f temp,
        @Local ModelPart.Polygon polygon
    ) {
        if (buffer instanceof VertexConsumerExtended vce) {
            vce.canpipe_setTangent((setter) -> {
                var tangent = (NormalAndTangent) polygon.normal();
                pose.transformNormal(tangent.tangentX, tangent.tangentY, tangent.tangentZ, temp);
                setter.set(temp.x, temp.y, temp.z, tangent.inverseBitangent);
            });
        }
    }

}
