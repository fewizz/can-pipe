package fewizz.canpipe.mixin.m03_uniform_ivec2;

import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.shaders.UniformType;

@Mixin(GlProgram.class)
public class GlProgramMixin {

    @ModifyReturnValue(method = "getTypeFromGl", at = @At("RETURN"))
    private static UniformType getTypeFromGl(UniformType result, @Local int glConst) {
        if (glConst == GL33C.GL_INT_VEC2) {
            return UniformType.valueOf("IVEC2");
        }
        return result;
    }

}
