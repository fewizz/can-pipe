package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.shaders.Uniform;

import fewizz.canpipe.pipeline.ProgramBase;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderProgramConfig;

@Mixin(CompiledShaderProgram.class)
public class CompiledShaderProgramMixin {

    @Shadow
    @Final
    private Object2IntMap<String> samplerTextures;

    @WrapOperation(
        method = "apply",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;bindTexture(I)V",
            remap = false
        )
    )
    void onApplyTexureBind(int id, Operation<Void> original) {
        if (((Object) this) instanceof ProgramBase pb) {
            pb.onTextureBindOnApply(id);
        }
        else {
            original.call(id);
        }
    }

    @Inject(
        method = "clear",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Ljava/util/List;get(I)Ljava/lang/Object;",
            remap = false,
            shift = Shift.BY,
            by = 2
        )
    )
    void onSamplerClear(
        CallbackInfo ci,
        @Local ShaderProgramConfig.Sampler sampler,
        @Local(ordinal = 1) int textureUnit
    ) {
        if ((Object) this instanceof ProgramBase pb) {
            int id = this.samplerTextures.getOrDefault(sampler.name(), -1);
            if (id != -1) {
                pb.onClearSampler(id, textureUnit);
            }
        }
    }

    @WrapOperation(
        method = "apply",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/shaders/Uniform;upload()V"
        )
    )
    void onUniformApply(
        Uniform instance,
        Operation<Void> original
    ) {
        if ((Object) this instanceof ProgramBase pb && pb.manuallyAppliedUniforms.contains(instance)) {
            return;
        }
        original.call(instance);
    }

}
