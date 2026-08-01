package fewizz.canpipe.b3d.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.mojang.blaze3d.opengl.DirectStateAccess;

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
