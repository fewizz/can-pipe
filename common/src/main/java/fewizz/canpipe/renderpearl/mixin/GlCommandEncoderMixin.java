package fewizz.canpipe.renderpearl.mixin;

import org.joml.Vector4f;
import org.lwjgl.opengl.GL33C;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.backend.opengl.GlCommandEncoder;
import com.mojang.renderpearl.backend.opengl.GlDevice;
import com.mojang.renderpearl.backend.opengl.GlRenderPass;
import com.mojang.renderpearl.backend.opengl.GlRenderPipeline;
import com.mojang.renderpearl.backend.opengl.GlStateManager;
import com.mojang.renderpearl.backend.opengl.GlTexture;
import com.mojang.renderpearl.backend.opengl.Uniform;

import fewizz.canpipe.renderpearl.CommandEncoderBackendExtended;

@Mixin(GlCommandEncoder.class)
public abstract class GlCommandEncoderMixin implements CommandEncoderBackendExtended {

    @Shadow @Final private int readFbo;
    @Shadow @Final private int drawFbo;
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private GlDevice device;

    @Unique private int canpipe_clearBaseLevel = -1;
    @Unique private int canpipe_clearLevelCount = -1;
    @Unique private int canpipe_clearBaseLayer = -1;
    @Unique private int canpipe_clearLayerCount = -1;
    @Unique private int canpipe_copySrcLayer = -1;
    @Unique private int canpipe_copyDstLayer = -1;

    @ModifyExpressionValue(
        method = "setupDraw",
        at = @At(value = "CONSTANT", args = "intValue=3553")  // GL_TEXTURE_2D
    )
    int fixTextureTarget(int target, @Local GlTexture glTexture) {
        return GlStateManagerAccessor.canpipe_getTextureTarget(glTexture.glId());
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
        GpuTexture texture, Vector4f color, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount
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

    @Override
    public void canpipe_copyTextureToTexture(
        GpuTexture source, GpuTexture destination, int mipLevel, int destX, int destY, int sourceX, int sourceY, int width, int height,
        int sourceLayer, int destinationLayer
    ) {
        try {
            this.canpipe_copySrcLayer = sourceLayer;
            this.canpipe_copyDstLayer = destinationLayer;
            this.copyTextureToTexture(source, destination, mipLevel, destX, destY, sourceX, sourceY, width, height);
        }
        finally {
            this.canpipe_copySrcLayer = -1;
            this.canpipe_copyDstLayer = -1;
        }
    }

    @Override
    public void canpipe_blitImage(GpuTexture srcTexture, GpuTexture dstTexture) {
        GlStateManager._glBindFramebuffer(GL33C.GL_READ_FRAMEBUFFER, this.readFbo);
        GlStateManager._glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER, this.drawFbo);

        GlStateManager._glFramebufferTexture2D(GL33C.GL_READ_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0, GL33C.GL_TEXTURE_2D, ((GlTexture) srcTexture).glId(), 0);
        GlStateManager._glFramebufferTexture2D(GL33C.GL_DRAW_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0, GL33C.GL_TEXTURE_2D, ((GlTexture) dstTexture).glId(), 0);

        GL33C.glBlitFramebuffer(0, 0, srcTexture.getWidth(0), srcTexture.getHeight(0), 0, 0, dstTexture.getWidth(0), dstTexture.getHeight(0), GL33C.GL_COLOR_BUFFER_BIT, GL33C.GL_NEAREST);

        GlStateManager._glBindFramebuffer(GL33C.GL_READ_FRAMEBUFFER, 0);
        GlStateManager._glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER, 0);
    }

    @WrapOperation(
        method = "clearColorTexture",
        at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/opengl/GlStateManager;_clear(I)V")
    )
    public void clearNonZeroColorLayer(int mask, Operation<Void> operation, @Local(argsOnly = true) GpuTexture colorTexture) {
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
        at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/opengl/GlStateManager;_clear(I)V")
    )
    public void clearNonZeroDepthLayer(int mask, Operation<Void> operation, @Local(argsOnly = true) GpuTexture depthTexture) {
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

    @ModifyArg(
        method = "copyTextureToTexture",
        at = {
            @At(
                value = "INVOKE",
                target = "Lcom/mojang/renderpearl/backend/opengl/DirectStateAccess;bindFrameBufferTextures(IIIII)V",
                ordinal = 0  // src
            ),
            @At(  // NeoForge
                value = "INVOKE",
                target = "Lcom/mojang/renderpearl/backend/opengl/DirectStateAccess;bindFrameBufferTextures(IIIIIII)V",
                ordinal = 0  // src
            )
        },
        index = 3  // mipLevel
    )
    int setSrcLayer(int mipLevel) {
        if (this.canpipe_copySrcLayer != -1) {
            mipLevel |= this.canpipe_copySrcLayer << 16;
        }
        return mipLevel;
    }

    @ModifyArg(
        method = "copyTextureToTexture",
        at = {
            @At(
                value = "INVOKE",
                target = "Lcom/mojang/renderpearl/backend/opengl/DirectStateAccess;bindFrameBufferTextures(IIIII)V",
                ordinal = 1  // dst
            ),
            @At(  // NeoForge
                value = "INVOKE",
                target = "Lcom/mojang/renderpearl/backend/opengl/DirectStateAccess;bindFrameBufferTextures(IIIIIII)V",
                ordinal = 1  // dst
            )
        },
        index = 3  // mipLevel
    )
    int setDstLayer(int mipLevel) {
        if (this.canpipe_copyDstLayer != -1) {
            mipLevel |= this.canpipe_copyDstLayer << 16;
        }
        return mipLevel;
    }

    @ModifyExpressionValue(
        method = "setupDraw",
        at = {
            @At(
                value = "INVOKE",
                target = "Lit/unimi/dsi/fastutil/objects/ReferenceList;get(I)Ljava/lang/Object;",
                ordinal = 0
            ),
            @At(
                value = "INVOKE",
                target = "Lit/unimi/dsi/fastutil/objects/ReferenceList;get(I)Ljava/lang/Object;",
                ordinal = 1
            )
        }
    )
    public Object moreDetailedErrorOnNullUniform(
        Object uniformBufferSlice, @Local GlRenderPass renderPass, @Local(name = "i") int uniformIndex
    ) {
        if (uniformBufferSlice == null) {
            GlRenderPipeline glRenderPipeline = ((GlRenderPassAccessor) renderPass).canpipe_getPipeline();
            Uniform uniform = glRenderPipeline.program().getUniform(uniformIndex);
            if (uniform != null) {
                throw new IllegalStateException("Buffer for uniform #"+uniformIndex+" is null");
            }
        }
        return uniformBufferSlice;
    }

}
