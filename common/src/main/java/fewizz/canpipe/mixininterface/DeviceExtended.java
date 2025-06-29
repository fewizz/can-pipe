package fewizz.canpipe.mixininterface;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.commons.lang3.function.TriFunction;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;

import net.minecraft.resources.ResourceLocation;

public interface DeviceExtended {

    CompiledRenderPipeline canpipe_compilePipeline(
        RenderPipeline pipeline,
        BiFunction<ResourceLocation, ShaderType, String> shaderSource,
        TriFunction<ResourceLocation, String, ShaderType, String> preprocessor,
        Consumer<String> onCompilationError
    );

}
