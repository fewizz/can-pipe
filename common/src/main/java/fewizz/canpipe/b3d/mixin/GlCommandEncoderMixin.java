package fewizz.canpipe.b3d.mixin;

import java.nio.IntBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL33C;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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

import fewizz.canpipe.GFX;
import fewizz.canpipe.b3d.CommandEncoderExtended;
import fewizz.canpipe.b3d.GpuTextureExtended;
import fewizz.canpipe.b3d.GpuTextureViewExtended;
import fewizz.canpipe.b3d.TextureType;
import net.minecraft.util.ARGB;

@Mixin(GlCommandEncoder.class)
public abstract class GlCommandEncoderMixin implements CommandEncoderExtended {

    @Unique private VertexFormatElement.Type canpipe_type = null;
    @Unique private List<GlTextureView> canpipe_colorAttachements = null;
    @Unique private int canpipe_clearDepthBaseMipLevel = -1;
    @Unique private int canpipe_clearDepthLevelCount = -1;
    @Unique private int canpipe_clearDepthBaseArrayLayer = -1;
    @Unique private int canpipe_clearDepthLayerCount = -1;

    @Shadow private boolean inRenderPass;
    @Shadow @Final private static Logger LOGGER = LogUtils.getLogger();
    @Shadow @Final private GlDevice device;
    @Shadow private RenderPipeline lastPipeline;
    @Shadow @Final private int drawFbo;

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

    @Override
    public RenderPass canpipe_createRenderPass(
        Supplier<String> supplier,
        List<GpuTextureView> colorAttachments,
        @Nullable GpuTextureView depthAttachment
    ) {
        try {
            this.canpipe_colorAttachements = colorAttachments.stream().map(a -> (GlTextureView)a).toList();
            return this.createRenderPass(
                supplier, this.canpipe_colorAttachements.size() > 0 ? this.canpipe_colorAttachements.get(0) : null, OptionalInt.empty(),
                depthAttachment, OptionalDouble.empty()
            );
        }
        finally {
            this.canpipe_colorAttachements = null;
        }
    }

    @Overwrite
    @Override
    public RenderPass createRenderPass(
        Supplier<String> supplier, GpuTextureView gpuTextureView, OptionalInt optionalInt, @Nullable GpuTextureView depthTextureView, OptionalDouble optionalDouble
    ) {
        // if (gpuTextureView == null) {
            // CanPipe.trap();
        // }
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else {
            if (optionalDouble.isPresent() && depthTextureView == null) {
                LOGGER.warn("Depth clear value was provided but no depth texture is being used");
            }

            if (gpuTextureView != null && gpuTextureView.isClosed()) {
                throw new IllegalStateException("Color texture is closed");
            } else if (gpuTextureView != null && (gpuTextureView.texture().usage() & 8) == 0) {
                throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
            } else if (gpuTextureView != null && gpuTextureView.texture().getDepthOrLayers() > 1) {
                throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported as an attachment");
            } else {
                if (depthTextureView != null) {
                    if (depthTextureView.isClosed()) {
                        throw new IllegalStateException("Depth texture is closed");
                    }

                    if ((depthTextureView.texture().usage() & 8) == 0) {
                        throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
                    }

                    /*if (gpuTextureView2.texture().getDepthOrLayers() > 1) {
                        throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported as an attachment");
                    }*/
                }

                this.inRenderPass = true;
                this.device.debugLabels().pushDebugGroup(supplier);

                int fboID;
                if (this.canpipe_colorAttachements != null) {
                    var cache = ((GlDeviceAccessor) this.device).get_canpipe_framebufferCache();
                    fboID = cache.computeIfAbsent(Pair.of(this.canpipe_colorAttachements, (GlTextureView) depthTextureView), (Pair<List<GlTextureView>, GlTextureView> attachments) -> {
                        int id = GlStateManager.glGenFramebuffers();
                        var colorAttachments = attachments.getLeft();
                        var depthAttachment = attachments.getRight();

                        GlStateManager._glBindFramebuffer(GL33C.GL_FRAMEBUFFER, id);
                        GFX.glDrawBuffers(IntStream.range(0, colorAttachments.size()).map(i -> GL33C.GL_COLOR_ATTACHMENT0+i).toArray());

                        for (int attachmentIndex = 0; attachmentIndex < colorAttachments.size(); ++attachmentIndex) {
                            var attachment = colorAttachments.get(attachmentIndex);
                            var attachmentExt = (GpuTextureViewExtended) attachment;

                            var textureType = ((GpuTextureExtended) attachment.texture()).canpipe_getType();
                            var textureID = ((GlTextureView) attachment).texture().glId();

                            if (textureType == TextureType.TYPE_2D) {
                                GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + attachmentIndex, GL33C.GL_TEXTURE_2D, textureID, attachment.baseMipLevel());
                            } else if (textureType == TextureType.TYPE_2D) {
                                GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + attachmentIndex, textureID, attachment.baseMipLevel(), attachmentExt.canpipe_baseArrayLayer());
                            } else if (textureType == TextureType.TYPE_CUBE_MAP) {
                                int face = attachmentExt.canpipe_baseArrayLayer() % 6;
                                // int layer = attachmentExt.canpipe_baseArrayLayer() / 6;
                                GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + attachmentIndex, GL33C.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, textureID, attachment.baseMipLevel());
                            } /* else if (attachment.textureView.target == GL40C.GL_TEXTURE_CUBE_MAP_ARRAY) {
                                GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + attachmentIndex, textureID, attachment.lod, attachment.layer * 6 + attachment.face);
                            } */ else {
                                throw new NotImplementedException();
                            }
                        }

                        if (depthAttachment != null) {
                            var textureType = ((GpuTextureExtended) depthAttachment.texture()).canpipe_getType();
                            var textureID = ((GlTextureView) depthAttachment).texture().glId();

                            if (textureType == TextureType.TYPE_2D) {
                                GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, GL33C.GL_TEXTURE_2D, textureID, depthAttachment.baseMipLevel());
                            } else if (textureType == TextureType.TYPE_2D_ARRAY) {
                                GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, textureID, depthAttachment.baseMipLevel(), ((GpuTextureViewExtended) depthAttachment).canpipe_baseArrayLayer());
                            } else {
                                throw new NotImplementedException();
                            }
                        }
                        return id;
                    });
                }
                else {
                    fboID = ((GlTexture) gpuTextureView.texture()).getFbo(this.device.directStateAccess(), depthTextureView == null ? null : depthTextureView.texture());
                }

                GlStateManager._glBindFramebuffer(GL33C.GL_FRAMEBUFFER, fboID);
                int j = 0;
                if (optionalInt.isPresent()) {
                    int k = optionalInt.getAsInt();
                    GL11.glClearColor(ARGB.redFloat(k), ARGB.greenFloat(k), ARGB.blueFloat(k), ARGB.alphaFloat(k));
                    j |= 16384;
                }

                if (depthTextureView != null && optionalDouble.isPresent()) {
                    GL11.glClearDepth(optionalDouble.getAsDouble());
                    j |= 256;
                }

                if (j != 0) {
                    GlStateManager._disableScissorTest();
                    GlStateManager._depthMask(true);
                    GlStateManager._colorMask(true, true, true, true);
                    GlStateManager._clear(j);
                }

                int width = gpuTextureView != null ? gpuTextureView.getWidth(0) : depthTextureView.getWidth(0);
                int height = gpuTextureView != null ? gpuTextureView.getHeight(0) : depthTextureView.getHeight(0);

                GlStateManager._viewport(0, 0, width, height);
                this.lastPipeline = null;
                return new GlRenderPass((GlCommandEncoder)(Object)this, depthTextureView != null);
            }
        }
    }

    @Override
    public void canpipe_clearDepthTexture(
        GpuTexture texture, double depth, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount
    ) {
        try {
            this.canpipe_clearDepthBaseMipLevel = baseMipLevel;
            this.canpipe_clearDepthLevelCount = levelCount;
            this.canpipe_clearDepthBaseArrayLayer = baseArrayLayer;
            this.canpipe_clearDepthLayerCount = layerCount;
            this.clearDepthTexture(texture, depth);
        }
        finally {
            this.canpipe_clearDepthBaseMipLevel = -1;
            this.canpipe_clearDepthLevelCount = -1;
            this.canpipe_clearDepthBaseArrayLayer = -1;
            this.canpipe_clearDepthLayerCount = -1;
        }
    }

    @WrapOperation(
        method = "clearDepthTexture",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_clear(I)V"
        )
    )
    public void clearNonZeroDepthLayer(int mask, Operation<Void> original, @Local GpuTexture depthTexture) {
        if (this.canpipe_clearDepthBaseArrayLayer != -1) {
            var glTexture = (GlTexture) depthTexture;

            int minLevel = this.canpipe_clearDepthBaseMipLevel != -1 ? this.canpipe_clearDepthBaseMipLevel : 0;
            int levels = this.canpipe_clearDepthLevelCount != -1 ? this.canpipe_clearDepthLevelCount : 1;

            int minLayer = this.canpipe_clearDepthBaseArrayLayer != -1 ? this.canpipe_clearDepthBaseArrayLayer : 0;
            int layers = this.canpipe_clearDepthLayerCount != -1 ? this.canpipe_clearDepthLayerCount : 1;

            for (int level = minLevel; level < minLevel + levels; ++level) {
                for (int layer = minLayer; layer < minLayer + layers; ++layer) {
                    GL33C.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, glTexture.glId(), level, layer);
                    GlStateManager._clear(GL33C.GL_DEPTH_BUFFER_BIT);
                }
            }
        }
        else {
            original.call(mask);
        }
    }

    @Overwrite
    private void verifyDepthTexture(GpuTexture texture) {
        if (!texture.getFormat().hasDepthAspect()) {
            throw new IllegalStateException("Trying to clear a non-depth texture as depth");
        } else if (texture.isClosed()) {
            throw new IllegalStateException("Depth texture is closed");
        } else if ((texture.usage() & 8) == 0) {
            throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
        }/* else if (texture.getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Clearing a texture with multiple layers or depths is not yet supported");
        }*/
    }

}
