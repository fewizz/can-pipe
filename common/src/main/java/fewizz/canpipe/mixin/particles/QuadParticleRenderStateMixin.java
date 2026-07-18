package fewizz.canpipe.mixin.particles;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.material.Material;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.mixininterface.QuadParticleRenderStateExtended;
import fewizz.canpipe.mixininterface.QuadParticleRenderStateStorageExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState.Storage;

@Mixin(QuadParticleRenderState.class)
public class QuadParticleRenderStateMixin implements QuadParticleRenderStateExtended {

    @Unique private Material canpipe_pendingMaterial;
    @Unique private short[] canpipe_materialsIndices;
    @Unique private int canpipe_forEachParticleIndex;

    @Override
    public void canpipe_setPendingMaterial(Material material) {
        this.canpipe_pendingMaterial = material;
    }

    @Shadow @Final private Map<SingleQuadParticle.Layer, Storage> particles;

    @Inject(
        method = "add",
        at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/state/level/QuadParticleRenderState$Storage.add(FFFFFFFFFFFFII)V", shift = Shift.AFTER)
    )
    void afterAdd(CallbackInfo ci, @Local SingleQuadParticle.Layer layer) {
        ((QuadParticleRenderStateStorageExtended) this.particles.get(layer)).canpipe_alsoAddMaterial(this.canpipe_pendingMaterial);
    }


    @ModifyExpressionValue(
        method = "buildLayer",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;",
            ordinal = 0
        )
    )
    Object onGetStorage(Object storage) {
        this.canpipe_materialsIndices = ((QuadParticleRenderStateStorageExtended) storage).canpipe_materialsValues();
        this.canpipe_forEachParticleIndex = 0;
        return storage;
    }

    @Inject(
        method = "lambda$buildLayer$0",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;renderRotatedQuad(Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFFFFFII)V"
        )
    )
    void beforeRenderRotatedQuad(CallbackInfo ci, @Local(argsOnly = true) VertexConsumer vertexConsumer) {
        if (vertexConsumer instanceof VertexConsumerExtended vce) {
            Material material = Materials.get(this.canpipe_materialsIndices[this.canpipe_forEachParticleIndex]);
            if (material != null) {
                vce.canpipe_setPendingMaterialIndex(material.index());
            }
            ++this.canpipe_forEachParticleIndex;
        }
    }

}
