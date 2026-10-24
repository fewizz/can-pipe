package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import fewizz.canpipe.mixininterface.LevelRendererExtended;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.culling.Frustum;

@Mixin(SectionOcclusionGraph.class)
public class SectionOcclusionGraphMixin {

    @WrapMethod(
        method = "offsetFrustum"
    )
    private static Frustum dontOffsetShadowFrustum(Frustum frustum, Operation<Frustum> operation) {
        var mc = Minecraft.getInstance();
        if (((LevelRendererExtended) mc.levelRenderer).canpipe_getCurrentShadowCascadeIdx() >= 0) {
            return frustum;
        }
        return operation.call(frustum);
    }

}
