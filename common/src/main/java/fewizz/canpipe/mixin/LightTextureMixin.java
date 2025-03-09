package fewizz.canpipe.mixin;

import java.nio.ByteBuffer;

import org.joml.Vector4f;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.platform.GlStateManager;

import fewizz.canpipe.mixininterface.LightTextureExtended;
import net.minecraft.client.renderer.LightTexture;

@Mixin(LightTexture.class)
public class LightTextureMixin implements LightTextureExtended {

    @Unique private float darknessScale = 1.0F;
    @Unique private ByteBuffer emissiveColor = MemoryUtil.memAlloc(4*Float.BYTES);

    @Override
    public float canpipe_getDarknessScale() {
        return this.darknessScale;
    }

    @Override
    public Vector4f canpipe_getEmissiveColor() {
        return new Vector4f(emissiveColor);
    }

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

    @Inject(
        method = "updateLightTexture",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/pipeline/TextureTarget;unbindWrite()V"
        )
    )
    void fetchEmissiveColor(CallbackInfo ci) {
        GlStateManager._readPixels(15, 15, 1, 1, GL33C.GL_RGBA, GL33C.GL_FLOAT, emissiveColor);
    }

}
