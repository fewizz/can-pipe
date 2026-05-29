package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;

import fewizz.canpipe.material.EntityMaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @WrapOperation(
        method = "submit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit("+
                "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"+
                "Lcom/mojang/blaze3d/vertex/PoseStack;"+
                "Lnet/minecraft/client/renderer/SubmitNodeCollector;"+
                "Lnet/minecraft/client/renderer/state/level/CameraRenderState;"+
            ")V"
        )
    )
    void onSubmit(
        EntityRenderer<?, ?> instance,
        EntityRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera,
        Operation<Void> operation
    ) {
        try {
            EntityMaterialMap materialMap = MaterialMaps.getForEntity(state.entityType);
            ((SubmitNodeCollectorExtended) submitNodeCollector).canpipe_setScopedModelMaterialMap(materialMap);
            operation.call(instance, state, poseStack, submitNodeCollector, camera);
        } finally {
            ((SubmitNodeCollectorExtended) submitNodeCollector).canpipe_setScopedModelMaterialMap(null);
        }
    }

}
