package fewizz.canpipe.renderpearl;

import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;

import com.mojang.renderpearl.backend.api.SpvModule;

import net.minecraft.resources.Identifier;

public record ExtraPipelineBuilderOptions(

    // If true, interface variable locations (`out` variables in a vertex shader, `in` variables in a fragment shader) will be recomputed
    boolean canpipe_computeLocations,

    // If specified, will be called instead of raising ShaderCompileException (with message Unable to find shader defined uniform X)
    @Nullable TriConsumer<
        Identifier,  // Pipeline location
        String,  // Shader name
        SpvModule.Reflection.Descriptor  // Missing descriptor
    > canpipe_onUnhandledUniform,

    // If specified, will be called on `shaderc_compile_into_spv()` error
    @Nullable TriConsumer<
        String,  // Error log
        Identifier,  // Location of a shader
        String  // Full src of a shader
    > canpipe_spvCompilationErrorListener

) {

}
