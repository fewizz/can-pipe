package fewizz.canpipe.compat.indigo.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.compat.indigo.mixininterface.MutableQuadViewExtended;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractBlockRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

@Mixin(value=AbstractBlockRenderContext.class)
public class AbstractBlockRenderContextMixin {

    @Final
    @Shadow
    protected AoCalculator aoCalc;

    @Final
    @Shadow
    protected BlockRenderInfo blockInfo;

    @Inject(
        method = "bufferQuad",
        at = @At(
            value = "INVOKE",
            target = "Lnet/fabricmc/fabric/impl/client/indigo/renderer/render/AbstractBlockRenderContext;bufferQuad("+
                "Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;"+
                "Lcom/mojang/blaze3d/vertex/VertexConsumer;"+
            ")V"
        )
    )
    void beforeVertexConsumerWrite(CallbackInfo ci, @Local VertexConsumer vc) {
        if (vc instanceof VertexConsumerExtended vce) {
            ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(blockInfo.blockState.getBlock());
            MaterialMap materialMap = MaterialMaps.BLOCKS.get(rl);
            if (materialMap != null && materialMap.defaultMaterial != null) {
                int index = Materials.id(materialMap.defaultMaterial);
                vce.setSharedMaterialIndex(index);
            }
        }
    }

    @Inject(
        method = "bufferQuad",
        at = @At(
            value = "INVOKE",
            target = "Lnet/fabricmc/fabric/impl/client/indigo/renderer/render/AbstractBlockRenderContext;bufferQuad("+
                "Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;"+
                "Lcom/mojang/blaze3d/vertex/VertexConsumer;"+
            ")V",
            shift = Shift.AFTER
        )
    )
    void afterVertexConsumerWrite(CallbackInfo ci, @Local VertexConsumer vc) {
        if (vc instanceof VertexConsumerExtended vce) {
            vce.resetSharedMaterialIndex();
        }
    }

    @WrapOperation(
        method = "shadeQuad",
        at = @At(
            value = "INVOKE",
            target = "Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;color(II)Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;"
        ),
        remap = false
    )
    MutableQuadViewImpl dontApplyAO(
        MutableQuadViewImpl instance,
        int vertexIndex,
        int newColor,
        Operation<MutableQuadViewImpl> original
    ) {
        // don't set color, set AO instead
        if (Pipelines.getCurrent() != null) {
            ((MutableQuadViewExtended) instance).setAO(vertexIndex, this.aoCalc.ao[vertexIndex]);
            return null;
        }
        return original.call(instance, vertexIndex, newColor);
    }

}
