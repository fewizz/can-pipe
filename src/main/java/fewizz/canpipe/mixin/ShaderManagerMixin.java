package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import fewizz.canpipe.Mod;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.ShaderProgram;

@Mixin(ShaderManager.class)
public class ShaderManagerMixin {

    @WrapMethod(method = "getProgram")
    CompiledShaderProgram wrapGetProgram(ShaderProgram shaderProgram, Operation<CompiledShaderProgram> original) {
        CompiledShaderProgram p = Mod.tryGetMaterialProgramReplacement(shaderProgram);
        if (p != null) {
            return p;
        }

        return original.call(shaderProgram);
    }

}
