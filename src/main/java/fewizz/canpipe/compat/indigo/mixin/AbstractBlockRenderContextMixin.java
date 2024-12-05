package fewizz.canpipe.compat.indigo.mixin;
/*
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import fewizz.canpipe.Pipelines;
import fewizz.canpipe.compat.indigo.mixininterface.MutableQuadViewExtended;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractBlockRenderContext;

@Mixin(value=AbstractBlockRenderContext.class, remap=false)*/
public class AbstractBlockRenderContextMixin {
/*
    @Final
    @Shadow
    protected AoCalculator aoCalc;

    @WrapOperation(
        method="shadeQuad",
        at=@At(
            value = "INVOKE",
            target = "Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;color(II)Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;"
        )
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
*/
}
