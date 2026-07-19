package fewizz.canpipe.b3d.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.glsl.GlslCompiler;

@Mixin(GlslCompiler.class)
public class VkGlslCompilerMixin {

    @ModifyExpressionValue(
        method = "createIntermediary",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/util/shaderc/Shaderc;shaderc_result_get_error_message(J)Ljava/lang/String;"
        )
    )
    String onCompilationError(String message) {
        GpuDeviceBackend device = ((GpuDeviceAccessor) RenderSystem.getDevice()).canpipe_getBackend();
        ((VkDeviceAccessor) device).set_canpipe_compilationLog(message);
        return message;
    }

}
