package fewizz.canpipe.mixin.depth;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.VulkanDevice;

import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;

@Mixin(VulkanDevice.class)
public class VkDeviceMixin {

    @ModifyArg(
        method = "compileShader",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vulkan/glsl/GlslCompiler;createIntermediary("+
                "Ljava/lang/String;"+
                "Ljava/lang/String;"+
                "Lcom/mojang/blaze3d/shaders/ShaderType;"+
            ")Lcom/mojang/blaze3d/vulkan/glsl/IntermediaryShaderModule;"
        ),
        index = 1
    )
    String changeClipDepth(String src, @Local VulkanDevice.ShaderCompilationKey key) {
        Pipeline p = Pipelines.getCurrent();
        if (  // TODO
            // p != null &&
            // !p.awareOfDepthRangeChanges &&
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
    }

}
