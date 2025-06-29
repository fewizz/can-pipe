package fewizz.canpipe.mixininterface;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;

import net.minecraft.resources.ResourceLocation;

public interface DeviceExtended {

    CompiledRenderPipeline canpipe_compilePipeline(
        RenderPipeline pipeline,
        BiFunction<ResourceLocation, ShaderType, String> shaderSource,
        Consumer<String> onCompilationError
    );

}
