package fewizz.canpipe.b3d.mixin;

import java.nio.IntBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlRenderPass;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.logging.LogUtils;

import fewizz.canpipe.b3d.CommandEncoderExtended;
import net.minecraft.util.ARGB;

@Mixin(GlCommandEncoder.class)
public abstract class GlCommandEncoderMixin implements CommandEncoderExtended {

    @Unique private VertexFormatElement.Type canpipe_type = null;
    @Unique private List<GlTextureView> canpipe_colorAttachements = null;

    @Shadow private boolean inRenderPass;
    @Shadow @Final private static Logger LOGGER = LogUtils.getLogger();
    @Shadow @Final private GlDevice device;
    @Shadow private RenderPipeline lastPipeline;

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

    @Overwrite
    @Override
    public RenderPass createRenderPass(
        Supplier<String> supplier, GpuTextureView gpuTextureView, OptionalInt optionalInt, @Nullable GpuTextureView gpuTextureView2, OptionalDouble optionalDouble
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else {
            if (optionalDouble.isPresent() && gpuTextureView2 == null) {
                LOGGER.warn("Depth clear value was provided but no depth texture is being used");
            }

            if (gpuTextureView.isClosed()) {
                throw new IllegalStateException("Color texture is closed");
            } else if ((gpuTextureView.texture().usage() & 8) == 0) {
                throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
            } else if (gpuTextureView.texture().getDepthOrLayers() > 1) {
                throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported as an attachment");
            } else {
                if (gpuTextureView2 != null) {
                    if (gpuTextureView2.isClosed()) {
                        throw new IllegalStateException("Depth texture is closed");
                    }

                    if ((gpuTextureView2.texture().usage() & 8) == 0) {
                        throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
                    }

                    if (gpuTextureView2.texture().getDepthOrLayers() > 1) {
                        throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported as an attachment");
                    }
                }

                this.inRenderPass = true;
                this.device.debugLabels().pushDebugGroup(supplier);

                if (this.canpipe_colorAttachements != null) {
                    ((GlDeviceAccessor) this.device).get_canpipe_framebufferCache();
                }

                int i = ((GlTexture)gpuTextureView.texture()).getFbo(this.device.directStateAccess(), gpuTextureView2 == null ? null : gpuTextureView2.texture());
                GlStateManager._glBindFramebuffer(36160, i);
                int j = 0;
                if (optionalInt.isPresent()) {
                    int k = optionalInt.getAsInt();
                    GL11.glClearColor(ARGB.redFloat(k), ARGB.greenFloat(k), ARGB.blueFloat(k), ARGB.alphaFloat(k));
                    j |= 16384;
                }

                if (gpuTextureView2 != null && optionalDouble.isPresent()) {
                    GL11.glClearDepth(optionalDouble.getAsDouble());
                    j |= 256;
                }

                if (j != 0) {
                    GlStateManager._disableScissorTest();
                    GlStateManager._depthMask(true);
                    GlStateManager._colorMask(true, true, true, true);
                    GlStateManager._clear(j);
                }

                GlStateManager._viewport(0, 0, gpuTextureView.getWidth(0), gpuTextureView.getHeight(0));
                this.lastPipeline = null;
                return new GlRenderPass((GlCommandEncoder)(Object)this, gpuTextureView2 != null);
            }
        }
    }

}
