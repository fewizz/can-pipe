package fewizz.canpipe.pipeline;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.KHRDebug;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.resources.ResourceLocation;

public class Framebuffer extends RenderTarget {

    public static record ColorAttachment(
        @NotNull Texture texture,
        @NotNull Vector4f clearColor,
        int lod,
        int layer
    ) {}

    public static record DepthAttachment(
        @NotNull Texture texture,
        @NotNull double clearDepth,
        Optional<Integer> lod,
        Optional<Integer> layer
    ) {}

    public final List<ColorAttachment> colorAttachments;
    public @Nullable final DepthAttachment depthAttachment;

    public Framebuffer(
        ResourceLocation pipelineLocation,
        String name,
        List<ColorAttachment> colorAttachments,
        @Nullable DepthAttachment depthAttachment
    ) {
        super(false /* useDepth */);
        this.colorAttachments = colorAttachments;
        this.depthAttachment = depthAttachment;
        createBuffers(0, 0);
        KHRDebug.glObjectLabel(
            GL33C.GL_FRAMEBUFFER,
            this.frameBufferId,
            pipelineLocation.toString()+"-"+name
        );
    }

    @Override
    public void destroyBuffers() {
        RenderSystem.assertOnRenderThreadOrInit();
        this.unbindRead();
        this.unbindWrite();

        this.depthBufferId = -1;
        this.colorTextureId = -1;

        if (this.frameBufferId > -1) {
            GlStateManager._glBindFramebuffer(36160, 0);
            GlStateManager._glDeleteFramebuffers(this.frameBufferId);
            this.frameBufferId = -1;
        }
    }

    @Override
    public void createBuffers(int w, int h) {
        var firstColor = colorAttachments.size() > 0 ? colorAttachments.get(0) : null;

        Vector3i extent = new Vector3i();
        int lod = 0;
        if (firstColor != null) {
            this.colorTextureId = firstColor.texture.getId();
            extent.max(firstColor.texture.extent);
            lod = Math.max(lod, firstColor.lod);
        }
        if (depthAttachment != null) {
            this.depthBufferId = depthAttachment.texture.getId();
            extent.max(depthAttachment.texture.extent);
            lod = Math.max(lod, depthAttachment.lod.orElse(0));
        }

        this.viewWidth = extent.x >> lod;
        this.viewHeight = extent.y >> lod;
        this.width = extent.x >> lod;
        this.height = extent.y >> lod;

        this.frameBufferId = GlStateManager.glGenFramebuffers();
        GlStateManager._glBindFramebuffer(GL33C.GL_FRAMEBUFFER, this.frameBufferId);
        if (colorAttachments.size() > 0) {
            GL33C.glDrawBuffers(IntStream.range(0, colorAttachments.size()).map(i -> GL33C.GL_COLOR_ATTACHMENT0+i).toArray());
        }
        else {
            GL33C.glDrawBuffer(GL33C.GL_NONE);
            GL33C.glReadBuffer(GL33C.GL_NONE);
        }

        for (int i = 0; i < colorAttachments.size(); ++i) {
            var a = colorAttachments.get(i);

            if (a.texture.target == GL33C.GL_TEXTURE_2D) {
                GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + i, a.texture.target, a.texture.getId(), a.lod);
            } else if (a.texture.target == GL33C.GL_TEXTURE_2D_ARRAY || a.texture.target == GL33C.GL_TEXTURE_3D) {
                GL33C.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + i, a.texture.getId(), a.lod, a.layer);
            } else if (a.texture.target == GL33C.GL_TEXTURE_CUBE_MAP) {
                GL33C.glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + i, GL33C.GL_TEXTURE_CUBE_MAP_POSITIVE_X + a.layer, a.texture.getId(), a.lod);
            } else {
                throw new NotImplementedException();
            }
        }

        if (depthAttachment != null) {
            if (depthAttachment.texture.target == GL33C.GL_TEXTURE_2D) {
                GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, depthAttachment.texture.target, depthAttachment.texture.getId(), depthAttachment.lod.orElse(0));
            } else if (depthAttachment.texture.target == GL33C.GL_TEXTURE_2D_ARRAY || depthAttachment.texture.target == GL33C.GL_TEXTURE_3D) {
                GL33C.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, depthAttachment.texture.getId(), depthAttachment.lod.orElse(0), depthAttachment.layer.orElse(0));
            } else {
                throw new NotImplementedException();
            }
        }

        this.checkStatus();
        this.unbindWrite();
    }

    @Override
    public void clear() {
        RenderSystem.assertOnRenderThreadOrInit();
        this.bindWrite(false);

        GL33C.glDrawBuffer(GL33C.GL_COLOR_ATTACHMENT0);

        super.clear();

        this.bindWrite(false);
        GL33C.glDrawBuffers(IntStream.range(0, colorAttachments.size()).map(i -> GL33C.GL_COLOR_ATTACHMENT0+i).toArray());
        this.unbindWrite();
    }

    /**
     * Called by <code>frex_clear</code>-type passes<p>
     * Note that {@link RenderTarget#clear} clears only first color and depth attachemnts
     */
    public void clearFully() {
        RenderSystem.assertOnRenderThreadOrInit();
        this.bindWrite(true);

        if (this.depthAttachment != null) {
            GlStateManager._clearDepth(depthAttachment.clearDepth);
            Texture depthTexture = depthAttachment.texture;

            if (depthTexture.target == GL33C.GL_TEXTURE_2D_ARRAY || depthTexture.target == GL33C.GL_TEXTURE_3D) {
                for (int lod = depthAttachment.lod.orElse(depthTexture.maxLod); lod >= depthAttachment.lod.orElse(0); --lod) {
                    for (int layer = depthAttachment.layer.orElse(depthTexture.extent.z-1); layer >= depthAttachment.layer.orElse(0); --layer) {
                        GL33C.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, depthTexture.getId(), lod, layer);
                        GlStateManager._clear(GL33C.GL_DEPTH_BUFFER_BIT);
                    }
                }
            }
            else {
                GlStateManager._clear(GL33C.GL_DEPTH_BUFFER_BIT);
            }
        }

        for (int i = 0; i < this.colorAttachments.size(); ++i) {
            var a = this.colorAttachments.get(i);
            GL33C.glDrawBuffer(GL33C.GL_COLOR_ATTACHMENT0 + i);
            GlStateManager._clearColor(a.clearColor.x, a.clearColor.y, a.clearColor.z, a.clearColor.w);
            GlStateManager._clear(GL33C.GL_COLOR_BUFFER_BIT);
        }

        GL33C.glDrawBuffers(IntStream.range(0, colorAttachments.size()).map(i -> GL33C.GL_COLOR_ATTACHMENT0+i).toArray());
        this.unbindWrite();
    }

}
