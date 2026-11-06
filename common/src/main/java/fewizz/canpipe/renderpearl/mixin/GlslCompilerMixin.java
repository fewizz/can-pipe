package fewizz.canpipe.renderpearl.mixin;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.function.TriConsumer;
import org.lwjgl.util.shaderc.Shaderc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.frontend.shaders.GlslCompiler;

import fewizz.canpipe.renderpearl.GlslCompilerExtended;
import net.minecraft.resources.Identifier;

@Mixin(GlslCompiler.class)
public abstract class GlslCompilerMixin implements GlslCompilerExtended {

    @Unique private final ThreadLocal<Boolean> canpipe_enableAutoMapLocations = ThreadLocal.withInitial(() -> null);
    @Unique private final ThreadLocal<TriConsumer<String, Identifier, String>> canpipe_compilationErrorListener = ThreadLocal.withInitial(() -> null);

    @Override
    public void canpipe_setEnableAutoMapLocationsForPendingCompilation(Boolean value) {
        this.canpipe_enableAutoMapLocations.set(value);
    }

    @Override
    public void canpipe_setCompilationErrorListenerForPendingCompilation(TriConsumer<String, Identifier, String> listener) {
        this.canpipe_compilationErrorListener.set(listener);
    }

    @ModifyReturnValue(method = "createBaseShaderOptions", at = @At("RETURN"))
    long enableAutoMapping(long shaderOptionsHandle) {
        Boolean enableAutoMapLocations = this.canpipe_enableAutoMapLocations.get();
        if (enableAutoMapLocations != null && enableAutoMapLocations) {
            this.canpipe_enableAutoMapLocations.set(false);

            Shaderc.shaderc_compile_options_set_auto_map_locations(shaderOptionsHandle, true);
        }
        return shaderOptionsHandle;
    }

    @ModifyExpressionValue(
        method = "compileToSpv",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/util/shaderc/Shaderc;shaderc_result_get_error_message(J)Ljava/lang/String;"
        )
    )
    String onCompilationError(
        String errorMessage,
        @Local(name = "sourceBuffer") ByteBuffer sourceBuffer,
        @Local(name = "name") String name
    ) {
        var listener = this.canpipe_compilationErrorListener.get();
        if (listener != null) {
            this.canpipe_compilationErrorListener.set(null);

            System.out.println("CALLING!!!!");
            byte[] bytes = new byte[sourceBuffer.limit()];
            sourceBuffer.get(0, bytes);
            String srcUtf8 = new String(bytes, StandardCharsets.UTF_8);
            listener.accept(errorMessage, Identifier.parse(name), srcUtf8);
        }
        return errorMessage;
    }

}
