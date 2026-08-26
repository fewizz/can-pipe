package fewizz.canpipe.renderpearl.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.mojang.renderpearl.backend.opengl.DirectStateAccess;

@Mixin(DirectStateAccess.class)
public class GlDirectStateAccessMixin {

    /*@ModifyExpressionValue(
        method = "create",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlHeuristics;isGlOnDx12()Z"
        )
    )
    private static boolean onCreate(boolean value) {
        return true;
    }*/

}
