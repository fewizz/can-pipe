package fewizz.canpipe.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;

import fewizz.canpipe.b3d.CommandEncoderExtended;
import net.minecraft.client.Screenshot;

@Mixin(Screenshot.class)
public class ScreenshotMixin {

    @ModifyExpressionValue(
        method = "takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;getColorTexture()Lcom/mojang/blaze3d/textures/GpuTexture;"
        )
    )
    private static GpuTexture replaceTextureIfFormatIsNotRGBA8(
        GpuTexture texture,
        @Share("canpipe_rgba8Texture") LocalRef<GpuTexture> canpipe_rgba8Texture
    ) {
        if (texture.getFormat() != GpuFormat.RGBA8_UNORM) {
            var device = RenderSystem.getDevice();
            canpipe_rgba8Texture.set(device.createTexture(
                () -> "RGBA8 Screenshot",
                GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST,
                GpuFormat.RGBA8_UNORM,
                texture.getWidth(0), texture.getHeight(0),
                1, 1
            ));

            ((CommandEncoderExtended) device.createCommandEncoder()).canpipe_blitImage(texture, canpipe_rgba8Texture.get());

            texture = canpipe_rgba8Texture.get();
        }

        return texture;
    }

    @WrapMethod(
        method = "takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V"
    )
    private static void afterTakingScreenshot(
        RenderTarget target, int downscaleFactor, Consumer<NativeImage> callback,
        Operation<Void> operation,
        @Share("canpipe_rgba8Texture") LocalRef<GpuTexture> canpipe_rgba8Texture
    ) {
        try {
            operation.call(target, downscaleFactor, callback);
        }
        finally {
            if (canpipe_rgba8Texture.get() != null) {
                canpipe_rgba8Texture.get().close();
            }
        }
    }

}
