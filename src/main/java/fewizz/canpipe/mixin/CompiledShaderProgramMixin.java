package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import fewizz.canpipe.pipeline.ProgramBase;
import net.minecraft.client.renderer.CompiledShaderProgram;

@Mixin(CompiledShaderProgram.class)
public class CompiledShaderProgramMixin {

    @WrapOperation(
        method = "apply",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;bindTexture(I)V",
            remap = false
        )
    )
    void onApplyTexureBind(int id, Operation<Void> original) {
        if (((Object) this) instanceof ProgramBase pb) {
            pb.onApplyTextureBind(id);
        }
        else {
            original.call(id);
        }
    }

}
