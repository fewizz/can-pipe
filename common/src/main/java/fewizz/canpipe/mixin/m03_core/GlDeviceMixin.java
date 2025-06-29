package fewizz.canpipe.mixin.m03_core;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.opengl.GlShaderModule;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;

import fewizz.canpipe.mixininterface.DeviceExtended;
import net.minecraft.resources.ResourceLocation;

@Mixin(GlDevice.class)
public abstract class GlDeviceMixin implements DeviceExtended {

    @Unique private Consumer<String> canpipe_onCompilationError = null;
    @Unique private String canpipe_compilationLog = null;

    @Shadow abstract GlRenderPipeline compilePipeline(RenderPipeline pipeline, BiFunction<ResourceLocation, ShaderType, String> shaderSource);

    @Override
    public CompiledRenderPipeline canpipe_compilePipeline(
        RenderPipeline pipeline,
        BiFunction<ResourceLocation, ShaderType, String> shaderSource,
        Consumer<String> onCompilationError
    ) {
        try {
            this.canpipe_onCompilationError = onCompilationError;
            return compilePipeline(pipeline, shaderSource);
        } finally {
            this.canpipe_onCompilationError = null;
            this.canpipe_compilationLog = null;
        }
    }

    @ModifyExpressionValue(
        method = "compileShader",
        at = @At(
            value = "CONSTANT",
            args = "intValue=32768"
        )
    )
    int extendMaxLogLength(int original, @Local(index = 0) int id) {
        int logLength = GlStateManager.glGetShaderi(id, GL33C.GL_INFO_LOG_LENGTH);
        return logLength;
    }

    @ModifyExpressionValue(
        method = "compileShader",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlStateManager;glGetShaderInfoLog(II)Ljava/lang/String;"
        )
    )
    String catchLog(String log) {
        this.canpipe_compilationLog = log;
        return log;
    }

    @ModifyReturnValue(method = "compileShader", at = @At("RETURN"))
    GlShaderModule onCompilationError(GlShaderModule module) {
        if (module == GlShaderModule.INVALID_SHADER && this.canpipe_onCompilationError != null) {
            this.canpipe_onCompilationError.accept(this.canpipe_compilationLog);
        }
        return module;
    }

}
