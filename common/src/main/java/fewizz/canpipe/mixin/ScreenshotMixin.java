package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;

import fewizz.canpipe.b3d.CommandEncoderExtended;
import net.minecraft.client.Screenshot;

@Mixin(Screenshot.class)
public class ScreenshotMixin {

    private static GpuTexture canpipe_rgba8Texture;

    @ModifyExpressionValue(
        method = "Lnet/minecraft/client/Screenshot;takeScreenshot("+
            "Lcom/mojang/blaze3d/pipeline/RenderTarget;"+
            "I"+
            "Ljava/util/function/Consumer;"+
        ")V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;getColorTexture()Lcom/mojang/blaze3d/textures/GpuTexture;"
        )
    )
    private static GpuTexture useRGBA8ImageFormat(GpuTexture texture) {
        // This is possible only if prev call to `takeScreenshot` thrown an exception
        // and then than exception was handled by callee
        if (canpipe_rgba8Texture != null) {
            canpipe_rgba8Texture.close();
        }

        if (texture.getFormat() != TextureFormat.RGBA8) {
            canpipe_rgba8Texture = RenderSystem.getDevice().createTexture(
                () -> "RGBA8 Screenshot",
                GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST,
                TextureFormat.RGBA8,
                texture.getWidth(0), texture.getHeight(0),
                1, 1
            );
            /*RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                texture, canpipe_rgba8Texture,
                0, 0, 0, 0, 0,
                texture.getWidth(0), texture.getHeight(0)
            );*/

            ((CommandEncoderExtended) RenderSystem.getDevice().createCommandEncoder()).canpipe_blitImage(
                texture,
                canpipe_rgba8Texture
            );

            texture = canpipe_rgba8Texture;
        }

        return texture;
    }

    @Inject(
        method = "takeScreenshot",
        at = @At("RETURN")
    )
    private static void afterTakingScreenshot(CallbackInfo ci) {
        if (canpipe_rgba8Texture != null) {
            canpipe_rgba8Texture.close();
        }
    }

}
