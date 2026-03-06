package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

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
        attachments.add(attachment);  // First is untouched, others are disabled

        for (int i = 1; i < shaderSet.attachmentCount(); ++i) {
            attachments.add(new HgGraphicsPipeline.Blend.Attachment(
                null,  // equations
                attachment.writeMask()
            ));
        }

        return attachments;
    }

    @ModifyArgs(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lgraphics/cinnabar/api/hg/HgGraphicsPipeline$ShaderSet$CreateInfo;gl("+
                "Ljava/lang/String;"+
                "Ljava/lang/String;"+
            ")Lgraphics/cinnabar/api/hg/HgGraphicsPipeline$ShaderSet$CreateInfo;"
        )
    )
    void patchShaders(Args args) {
        String vertexShader = args.get(0);
        String fragmentShader = args.get(1);

        vertexShader = vertexShader.replace("#define main realMain", "#undef main\n#define main realMain");

        vertexShader = vertexShader.replace(
            "layout(location = 6) CINNABAR_BETWEEN_STAGES flat float ChunkVisibility;",
            "layout(location = 16) CINNABAR_BETWEEN_STAGES flat float ChunkVisibility;"
        );
        vertexShader = vertexShader.replace(
            "layout(location = 7) CINNABAR_BETWEEN_STAGES flat ivec2 TextureSize;",
            "layout(location = 17) CINNABAR_BETWEEN_STAGES flat ivec2 TextureSize;"
        );

        vertexShader =
            "#define sample _sample\n"+  // `sampler` is reserved word
            "#define sampler _sampler\n\n"+

            "void canpipe_main();\n"+
            "void main() {\n"+
            "   canpipe_main();\n"+
            "   // NDC Z -1 <-> 1 => 0 <-> 1\n"+
            "   gl_Position.z = (gl_Position.z + gl_Position.w) * 0.5;\n"+
            "}\n"+
            "#define main canpipe_main\n\n"+
            vertexShader;

        fragmentShader = fragmentShader.replace(
            "layout(location = 6) CINNABAR_BETWEEN_STAGES flat float ChunkVisibility;",
            "layout(location = 16) CINNABAR_BETWEEN_STAGES flat float ChunkVisibility;"
        );
        fragmentShader = fragmentShader.replace(
            "layout(location = 7) CINNABAR_BETWEEN_STAGES flat ivec2 TextureSize;",
            "layout(location = 17) CINNABAR_BETWEEN_STAGES flat ivec2 TextureSize;"
        );

        fragmentShader =
            "#define sample _sample\n"+
            "#define sampler _sampler\n\n"+
            fragmentShader;

        args.set(0, vertexShader);
        args.set(1, fragmentShader);
    }

}
