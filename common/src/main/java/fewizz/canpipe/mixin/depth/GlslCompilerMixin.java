package fewizz.canpipe.mixin.depth;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.frontend.shaders.GlslCompiler;

import net.minecraft.client.renderer.ShaderDefines;

@Mixin(GlslCompiler.class)
public class GlslCompilerMixin {

    @ModifyArg(
        method = "compileToSpv",
        at = @At(ordinal = 0, value = "INVOKE", target = "Lorg/lwjgl/system/MemoryUtil;memUTF8(Ljava/lang/CharSequence;Z)Ljava/nio/ByteBuffer;"),
        index = 0
    )
    CharSequence beforeCompileToSpv(CharSequence src, @Local ShaderType type, @Local ShaderDefines shaderDefines) {
        if (
            type == ShaderType.VERTEX &&
            shaderDefines.flags().contains("CANPIPE_CLIP_DEPTH_RANGE_PATCH")
        ) {
            String newSrc = ((String) src).replaceAll(
                "void\s+main\\(\\)",
                "void canpipe_main()"
            );
            newSrc +=
                "\n\n"+
                "void main() {\n"+
                "   canpipe_main();\n"+
                "   gl_Position.z = (gl_Position.z + gl_Position.w) * 0.5;\n"+
                "}\n\n";
            src = newSrc;
        }

        return src;
    }

}
