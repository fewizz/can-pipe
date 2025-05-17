package fewizz.canpipe.mixin.m03_uniform_ivec2;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.opengl.AbstractUniform;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.shaders.UniformType;

import fewizz.canpipe.GFX;

@Mixin(Uniform.class)
public class UniformMixin extends AbstractUniform {

    @Shadow @Final private UniformType type;
    @Shadow @Final private IntBuffer intValues;
    @Shadow @Final private FloatBuffer floatValues;
    @Shadow private int location;
    @Shadow private boolean dirty;

    @Inject(method = "upload", at = @At("HEAD"), cancellable = true)
    void onUpload(CallbackInfo ci) {
        if (this.dirty && this.type.name().equals("IVEC2")) {
           GFX.glUniform2iv(this.location, this.intValues);
           this.dirty = false;
           ci.cancel();
        }
    }

    @Override  // They forgor to override it in 1.21.5
    public final void set(Vector4f values) {
        this.floatValues.position(0);
        values.get(this.floatValues);
        this.dirty = true;
    }

}
