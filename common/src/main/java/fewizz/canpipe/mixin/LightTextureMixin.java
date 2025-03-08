package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import fewizz.canpipe.mixininterface.LightTextureExtended;
import net.minecraft.client.renderer.LightTexture;

@Mixin(LightTexture.class)
public class LightTextureMixin implements LightTextureExtended {

    @Unique private float darknessScale = 1.0F;

    @ModifyExpressionValue(
        method = "updateLightTexture",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LightTexture;calculateDarknessScale("+
                "Lnet/minecraft/world/entity/LivingEntity;FF"+
            ")F"
        )
    )
    float saveDarknessScale(float darknessScale) {
        this.darknessScale = darknessScale;
        return darknessScale;
    }

    @Override
    public float canpipe_getDarknessScale() {
        return this.darknessScale;
    }

}
