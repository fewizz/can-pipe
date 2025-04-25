package fewizz.canpipe.mixin;

import java.nio.IntBuffer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.shaders.UniformType;

import fewizz.canpipe.GFX;

@Mixin(Uniform.class)
public class UniformMixin {

    private int location;
	@Final private UniformType type;
	@Final private IntBuffer intValues;

    @Shadow private boolean dirty;

    @Inject(method = "upload", at = @At("HEAD"), cancellable = true)
    void upload(CallbackInfo ci) {
        if (this.dirty && this.type.name().equals("IVEC2")) {
           GFX.glUniform2iv(this.location, this.intValues);
           this.dirty = false;
           ci.cancel();
        }
    }

}
