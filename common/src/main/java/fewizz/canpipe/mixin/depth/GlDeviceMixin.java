package fewizz.canpipe.mixin.depth;

import org.spongepowered.asm.mixin.Mixin;

import com.mojang.renderpearl.backend.opengl.GlDevice;

@Mixin(GlDevice.class)
public class GlDeviceMixin {

    // TODO
    /*@ModifyArg(
        method = "compileShader",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;glShaderSource(ILjava/lang/String;)V"),
        index = 1
    )
    String changeClipDepth(String src, @Local GlDevice.ShaderCompilationKey key) {
        PipelineRaw p = Pipelines.getLoadingError() == null ? Pipelines.getCurrentRaw() : null;
        if (
            p != null &&
            !p.awareOfDepthRangeChanges &&
            key.type() == ShaderType.VERTEX &&
            RenderSystem.getDevice().getDeviceInfo().isZZeroToOne()
        ) {
            src = src.replace(
                "void main()",
                "void canpipe_main()"
            );
            src +=
                "void main() {\n"+
                "   canpipe_main();\n"+
                "   gl_Position.z = (gl_Position.z + gl_Position.w) * 0.5; \n"+
                "}\n\n";
        }

        return src;
    }*/

}
