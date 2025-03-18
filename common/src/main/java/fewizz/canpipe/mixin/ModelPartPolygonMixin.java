package fewizz.canpipe.mixin;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.NormalAndTangent;
import net.minecraft.client.model.geom.ModelPart;


@Mixin(ModelPart.Polygon.class)
public class ModelPartPolygonMixin {

    @Shadow @Final private Vector3f normal;

    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/geom/ModelPart$Polygon;<init>("+
                "[Lnet/minecraft/client/model/geom/ModelPart$Vertex;"+
                "Lorg/joml/Vector3f;"+
            ")V"
        ),
        index = 1
    )
    static private Vector3f modifyNormal(Vector3f normal) {
        return new NormalAndTangent(normal);
    }

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    void computeTangent(CallbackInfo ci, @Local(argsOnly = true) ModelPart.Vertex[] vertices) {
        var result = NormalAndTangent.computeTangent(
            normal,
            vertices[0].pos().x, vertices[0].pos().y, vertices[0].pos().z, vertices[0].u(), vertices[0].v(),
            vertices[1].pos().x, vertices[1].pos().y, vertices[1].pos().z, vertices[1].u(), vertices[1].v(),
            vertices[2].pos().x, vertices[2].pos().y, vertices[2].pos().z, vertices[2].u(), vertices[2].v()
        );
        var normalAndTangent = ((NormalAndTangent) normal);
        normalAndTangent.tangentX = result.getLeft().x;
        normalAndTangent.tangentY = result.getLeft().y;
        normalAndTangent.tangentZ = result.getLeft().z;
        normalAndTangent.inverseBitangent = result.getRight();
    }

}
