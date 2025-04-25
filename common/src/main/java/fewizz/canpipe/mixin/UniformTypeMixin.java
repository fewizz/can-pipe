package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.shaders.UniformType;

@Mixin(UniformType.class)
public class UniformTypeMixin {

    @Shadow
    @Final
    String name;

    @ModifyReturnValue(method = "isIntStorage", at = @At("RETURN"))
    public boolean isIntStorage(boolean original) {
        return original || this.name.equals("ivec2");
    }

}
