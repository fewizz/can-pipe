package fewizz.canpipe.compat.cinnabar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.renderer.GameRenderer;

@Mixin(value = GameRenderer.class, priority = 1001)
public class MCGameRendererMixin {

    @ModifyArg(
        method = "getProjectionMatrix",
        at = @At(
            value = "INVOKE",
            target = "Lorg/joml/Matrix4f;perspective(FFFFZ)Lorg/joml/Matrix4f;"
        ),
        require = 0, expect = 0,
        index = 4
    )
    private boolean overrideZZeroToOne(boolean zZeroToOne) {
        return false;  // Handling in vertex shader
    }

}
