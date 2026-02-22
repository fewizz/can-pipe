package fewizz.canpipe.compat.cinnabar.mixin;
/*
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
*/
/*@Mixin(value = CachedOrthoProjectionMatrixBuffer.class, priority = 1001)*/
public class MCCachedOrthoProjectionMatrixBufferMixin {
/*
    @ModifyArg(
        method = "createProjectionMatrix",
        at = @At(
            value = "INVOKE",
            target = "Lorg/joml/Matrix4f;setOrtho(FFFFFFZ)Lorg/joml/Matrix4f;"
        ),
        require = 0, expect = 0,
        index = 6
    )
    private boolean overrideZZeroToOne(boolean zZeroToOne) {
        return false;  // Handling in vertex shader
    }
*/
}
