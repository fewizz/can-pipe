package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;

import graphics.cinnabar.api.hg.HgGraphicsPipeline;
import graphics.cinnabar.core.hg3d.Hg3DRenderPipeline;

@Mixin(Hg3DRenderPipeline.class)
public class Hg3DRenderPipelineMixin {

    @Final private HgGraphicsPipeline.ShaderSet shaderSet;

    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lgraphics/cinnabar/api/hg/HgGraphicsPipeline$Blend;<init>(Ljava/util/List;Lorg/joml/Vector4fc;)V"
        ),
        index = 0
    )
    List<HgGraphicsPipeline.Blend.Attachment> replaceBlendAttachments(
        List<HgGraphicsPipeline.Blend.Attachment> attachments,
        @Local HgGraphicsPipeline.Blend.Attachment attachment
    ) {
        assert attachments.size() == 1 && attachments.get(0) == attachment;

        attachments = new ArrayList<>();
        attachments.add(attachment);
        for (int i = 1; i < shaderSet.attachmentCount(); ++i) {
            attachments.add(new HgGraphicsPipeline.Blend.Attachment(
                null,  // equations
                attachment.writeMask()
            ));
        }
        return attachments;
    }

    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lgraphics/cinnabar/api/hg/HgGraphicsPipeline$ShaderSet$CreateInfo;gl("+
                "Ljava/lang/String;"+
                "Ljava/lang/String;"+
            ")Lgraphics/cinnabar/api/hg/HgGraphicsPipeline$ShaderSet$CreateInfo;"
        ),
        index = 0
    )
    String patchVertexShader(String vertexShader) {
        vertexShader =
            "void canpipe_main();\n"+
            "void main() {\n"+
            "   canpipe_main();\n"+
            "   // NDC Z -1 <-> 1 => 0 <-> 1\n"+
            "   gl_Position.z = (gl_Position.z + gl_Position.w) * 0.5;\n"+
            "}\n"+
            "#define main canpipe_main\n\n"+
            vertexShader;

        // patch Cinnabar's dynamictransforms.glsl
        vertexShader = vertexShader.replace(
            "#define main realMain",
            "#ifdef main\n"+
            "   #undef main\n"+
            "#endif\n"+
            "#define main realMain\n"
        );

        return vertexShader;
    }

}
