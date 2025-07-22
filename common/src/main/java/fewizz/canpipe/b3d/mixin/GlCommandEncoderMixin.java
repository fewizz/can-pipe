package fewizz.canpipe.b3d.mixin;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL33C;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.logging.LogUtils;

import fewizz.canpipe.b3d.CommandEncoderExtended;
import fewizz.canpipe.b3d.GpuTextureViewExtended;

@Mixin(GlCommandEncoder.class)
public abstract class GlCommandEncoderMixin implements CommandEncoderExtended {

    @Unique private GpuTextureView[] canpipe_colorAttachements = null;
    @Unique private int canpipe_clearBaseLevel = -1;
    @Unique private int canpipe_clearLevelCount = -1;
    @Unique private int canpipe_clearBaseLayer = -1;
    @Unique private int canpipe_clearLayerCount = -1;

    @Shadow private boolean inRenderPass;
    @Shadow @Final private static Logger LOGGER = LogUtils.getLogger();
    @Shadow @Final private GlDevice device;
    @Shadow private RenderPipeline lastPipeline;

    @Override
    public RenderPass canpipe_createRenderPass(
        Supplier<String> supplier,
        GpuTextureView[] colorAttachments,
        @Nullable GpuTextureView depthAttachment
    ) {
        try {
            this.canpipe_colorAttachements = colorAttachments;
            return this.createRenderPass(
                supplier, this.canpipe_colorAttachements.length > 0 ? this.canpipe_colorAttachements[0] : null, OptionalInt.empty(),
                depthAttachment, OptionalDouble.empty()
            );
        }
        finally {
            this.canpipe_colorAttachements = null;
        }
    }

    @ModifyExpressionValue(
        method = "trySetup",
        at = @At(value = "CONSTANT", args = "intValue=3553")  // GL_TEXTURE_2D
    )
    int fixTextureTarget(int target, @Local GlTexture glTexture) {
        return GlStateManagerAccessor.canpipe_getTextureTarget(glTexture.glId());
    }

    @ModifyExpressionValue(
        method = "createRenderPass("+
            "Ljava/util/function/Supplier;"+
            "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
            "Ljava/util/OptionalInt;"+
            "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
            "Ljava/util/OptionalDouble;"+
        ")Lcom/mojang/blaze3d/systems/RenderPass;",
        at = {
            @At(
                value = "INVOKE",
                target = "Lcom/mojang/blaze3d/textures/GpuTexture;getDepthOrLayers()I",
                ordinal = 0
            ),
            @At(
                value = "INVOKE",
                target = "Lcom/mojang/blaze3d/textures/GpuTexture;getDepthOrLayers()I",
                ordinal = 1
            )
        }
    )
    int suppressMaxLayerCheckError(int layers) {
        return 0;
    }

    @Inject(
        method = "createRenderPass("+
            "Ljava/util/function/Supplier;"+
            "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
            "Ljava/util/OptionalInt;"+
            "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
            "Ljava/util/OptionalDouble;"+
        ")Lcom/mojang/blaze3d/systems/RenderPass;",
        at = @At("HEAD")
    )
    void replaceTextureViewIfNull(
        CallbackInfoReturnable<Void> ci,
        @Local(argsOnly = true, ordinal = 0) LocalRef<GpuTextureView> colorTextureView,
        @Local(argsOnly = true, ordinal = 1) GpuTextureView depthTextureView
    ) {
        if (colorTextureView.get() != null) return;

        if (this.canpipe_colorAttachements != null && this.canpipe_colorAttachements.length > 0) {
            colorTextureView.set(this.canpipe_colorAttachements[0]);
        }
        else {
            colorTextureView.set(depthTextureView);
        }
    }

    @WrapOperation(
        method = "createRenderPass("+
            "Ljava/util/function/Supplier;"+
            "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
            "Ljava/util/OptionalInt;"+
            "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
            "Ljava/util/OptionalDouble;"+
        ")Lcom/mojang/blaze3d/systems/RenderPass;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlTexture;getFbo(Lcom/mojang/blaze3d/opengl/DirectStateAccess;Lcom/mojang/blaze3d/textures/GpuTexture;)I"
        )
    )
    int ifColorAttachmentsCountNotEqualsOne(
        GlTexture colorTexture, DirectStateAccess dsa, GpuTexture depthTexture, Operation<Integer> operation,
        @Local(argsOnly = true, ordinal = 1) GpuTextureView depthTextureView
    ) {
        if (this.canpipe_colorAttachements == null) {
            return operation.call(colorTexture, dsa, depthTexture);
        }

        var fboCache = ((GlDeviceAccessor) this.device).get_canpipe_framebufferCache();

        return fboCache.computeIfAbsent(Pair.of(this.canpipe_colorAttachements, (GlTextureView) depthTextureView), (Pair<GpuTextureView[], GlTextureView> attachments) -> {
            int id = GlStateManager.glGenFramebuffers();
            var colorAttachments = attachments.getLeft();

            GlStateManager._glBindFramebuffer(GL33C.GL_FRAMEBUFFER, id);
            GL33C.glDrawBuffers(IntStream.range(0, colorAttachments.length).map(i -> GL33C.GL_COLOR_ATTACHMENT0+i).toArray());

            for (int attachmentIndex = 0; attachmentIndex < colorAttachments.length; ++attachmentIndex) {
                var attachment = colorAttachments[attachmentIndex];
                var attachmentExt = (GpuTextureViewExtended) attachment;

                var textureID = ((GlTextureView) attachment).texture().glId();

                if ((attachment.texture().usage() & GpuTexture.USAGE_CUBEMAP_COMPATIBLE) != 0) {
                    int face = attachmentExt.canpipe_baseArrayLayer() % 6;
                    int layer = attachmentExt.canpipe_baseArrayLayer() / 6;
                    if (layer > 0) { throw new RuntimeException(); }
                    GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + attachmentIndex, GL33C.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, textureID, attachment.baseMipLevel());
                }
                else if (attachment.texture().getDepthOrLayers() > 1 || attachmentExt.canpipe_baseArrayLayer() > 0) {
                    GL33C.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + attachmentIndex, textureID, attachment.baseMipLevel(), attachmentExt.canpipe_baseArrayLayer());
                } else {
                    GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + attachmentIndex, GL33C.GL_TEXTURE_2D, textureID, attachment.baseMipLevel());
                }
            }

            if (depthTextureView != null) {
                var depthAttachmentExt = (GpuTextureViewExtended) depthTextureView;
                var textureID = ((GlTextureView) depthTextureView).texture().glId();

                if (depthTextureView.texture().getDepthOrLayers() > 1 || depthAttachmentExt.canpipe_baseArrayLayer() > 0) {
                    GL33C.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, textureID, depthTextureView.baseMipLevel(), depthAttachmentExt.canpipe_baseArrayLayer());
                }
                else {
                    GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, GL33C.GL_TEXTURE_2D, textureID, depthTextureView.baseMipLevel());
                }
            }
            return id;
        });
    }

    @Override
    public void canpipe_clearDepthTexture(
        GpuTexture texture, double depth, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount
    ) {
        try {
            this.canpipe_clearBaseLevel = baseMipLevel;
            this.canpipe_clearLevelCount = levelCount;
            this.canpipe_clearBaseLayer = baseArrayLayer;
            this.canpipe_clearLayerCount = layerCount;
            this.clearDepthTexture(texture, depth);
        }
        finally {
            this.canpipe_clearBaseLevel = -1;
            this.canpipe_clearLevelCount = -1;
            this.canpipe_clearBaseLayer = -1;
            this.canpipe_clearLayerCount = -1;
        }
    }

    @Override
    public void canpipe_clearColorTexture(
        GpuTexture texture, int color, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount
    ) {
        try {
            this.canpipe_clearBaseLevel = baseMipLevel;
            this.canpipe_clearLevelCount = levelCount;
            this.canpipe_clearBaseLayer = baseArrayLayer;
            this.canpipe_clearLayerCount = layerCount;
            this.clearColorTexture(texture, color);
        }
        finally {
            this.canpipe_clearBaseLevel = -1;
            this.canpipe_clearLevelCount = -1;
            this.canpipe_clearBaseLayer = -1;
            this.canpipe_clearLayerCount = -1;
        }
    }

    @WrapOperation(
        method = "clearColorTexture",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_clear(I)V")
    )
    public void clearNonZeroColorLayer(int mask, Operation<Void> operation, @Local GpuTexture colorTexture) {
        if (this.canpipe_clearBaseLayer == -1) {
            operation.call(mask);
            return;
        }

        var glTexture = (GlTexture) colorTexture;

        for (int level = this.canpipe_clearBaseLevel; level < this.canpipe_clearBaseLevel + this.canpipe_clearLevelCount; ++level) {
            for (int layer = this.canpipe_clearBaseLayer; layer < this.canpipe_clearBaseLayer + this.canpipe_clearLayerCount; ++layer) {
                if (glTexture.getDepthOrLayers() > 1) {
                    GL33C.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0, glTexture.glId(), level, layer);
                }
                else {
                    int target = GlStateManagerAccessor.canpipe_getTextureTarget(glTexture.glId());
                    GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0, target, glTexture.glId(), level);
                }
                operation.call(mask);  // GlStateManager._clear(GL33C.GL_COLOR_BUFFER_BIT);
            }
        }
    }

    @WrapOperation(
        method = "clearDepthTexture",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_clear(I)V")
    )
    public void clearNonZeroDepthLayer(int mask, Operation<Void> operation, @Local GpuTexture depthTexture) {
        if (this.canpipe_clearBaseLayer == -1) {
            operation.call(mask);
            return;
        }

        var glTexture = (GlTexture) depthTexture;

        for (int level = this.canpipe_clearBaseLevel; level < this.canpipe_clearBaseLevel + this.canpipe_clearLevelCount; ++level) {
            for (int layer = this.canpipe_clearBaseLayer; layer < this.canpipe_clearBaseLayer + this.canpipe_clearLayerCount; ++layer) {
                if (glTexture.getDepthOrLayers() > 1) {
                    GL33C.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, glTexture.glId(), level, layer);
                }
                else {
                    int target = GlStateManagerAccessor.canpipe_getTextureTarget(glTexture.glId());
                    GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, target, glTexture.glId(), level);
                }
                operation.call(mask);  // GlStateManager._clear(GL33C.GL_DEPTH_BUFFER_BIT);
            }
        }
    }

    @ModifyExpressionValue(
        method = "verifyColorTexture",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/textures/GpuTexture;getDepthOrLayers()I")
    )
    private int allowColorTextureWithMultipleLayers(int layers) {
        if (this.canpipe_clearBaseLayer != -1) {
            layers = 1;  // replacing texture.getDepthOrLayers() with 1
        }
        return layers;
    }

    @ModifyExpressionValue(
        method = "verifyDepthTexture",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/textures/GpuTexture;getDepthOrLayers()I")
    )
    private int allowDepthTextureWithMultipleLayers(int layers) {
        if (this.canpipe_clearBaseLayer != -1) {
            layers = 1;  // replacing texture.getDepthOrLayers() with 1
        }
        return layers;
    }

}
