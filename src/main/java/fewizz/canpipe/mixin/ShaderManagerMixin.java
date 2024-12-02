package fewizz.canpipe.mixin;

import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.KHRDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.Pipelines;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.ShaderProgram;

@Mixin(ShaderManager.class)
public class ShaderManagerMixin {

    @WrapMethod(method = "getProgram")
    CompiledShaderProgram wrapGetProgram(ShaderProgram shaderProgram, Operation<CompiledShaderProgram> original) {
        var pipeline = Pipelines.getCurrent();
        if (pipeline != null) {
            var p = pipeline.materialPrograms.get(shaderProgram);
            if (p != null) {
                return p;
            }
        }

        return original.call(shaderProgram);
    }

    @ModifyExpressionValue(
        method = "linkProgram",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/CompiledShaderProgram;link("+
                "Lcom/mojang/blaze3d/shaders/CompiledShader;"+
                "Lcom/mojang/blaze3d/shaders/CompiledShader;"+
                "Lcom/mojang/blaze3d/vertex/VertexFormat;"+
            ")Lnet/minecraft/client/renderer/CompiledShaderProgram;"
        )
    )
    private static CompiledShaderProgram setProgramLabel(
        CompiledShaderProgram program,
        @Local ShaderProgram shaderProgram
    ) {
        KHRDebug.glObjectLabel(
            GL43C.GL_PROGRAM,
            program.getProgramId(),
            shaderProgram.configId().toString()
        );
        return program;
    }

}
