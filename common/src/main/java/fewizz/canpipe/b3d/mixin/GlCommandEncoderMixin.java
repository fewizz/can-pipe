package fewizz.canpipe.b3d.mixin;

import java.nio.IntBuffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import fewizz.canpipe.b3d.CommandEncoderExtended;

@Mixin(GlCommandEncoder.class)
public abstract class GlCommandEncoderMixin implements CommandEncoderExtended {

    @Unique private VertexFormatElement.Type canpipe_type = null;

    @ModifyExpressionValue(
        method = "writeToTexture("+
            "Lcom/mojang/blaze3d/textures/GpuTexture;"+
            "Ljava/nio/IntBuffer;"+
            "Lcom/mojang/blaze3d/platform/NativeImage$Format;"+
            "IIIIII"+
        ")V",
        at = @At(
            value = "CONSTANT",
            args = "intValue=5121"  // UNSIGNED_BYTE
        )
    )
    public int writeToTexture(int type) {
        if (this.canpipe_type != null) {
            type = GlConst.toGl(this.canpipe_type);
        }
        return type;
    }

    @Override
    public void canpipe_writeToTexture(
        GpuTexture gpuTexture, IntBuffer intBuffer, Format format, int i, int j, int k, int l, int m, int n,
        VertexFormatElement.Type type  // added
    ) {
        try {
            this.canpipe_type = type;
            // buffer size check will be incorrect, but anyway...
            this.writeToTexture(gpuTexture, intBuffer, format, i, j, k, l, m, n);
        } finally {
            this.canpipe_type = null;
        }
    }

    @ModifyExpressionValue(
        method = "trySetup",
        at = @At(
            value = "CONSTANT",
            args = "intValue=3553"  // GL_TEXTURE_2D
        )
    )
    int fixTextureTarget(int target, @Local GlTexture glTexture) {
        return GlStateManagerAccessor.canpipe_getTextureTarget(glTexture.glId());
    }

}
