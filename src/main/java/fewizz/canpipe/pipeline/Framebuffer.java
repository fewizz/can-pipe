package fewizz.canpipe.pipeline;

import java.util.List;
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
        int lod,
        int layer
    ) {}

    final List<ColorAttachment> colorAttachements;
    @Nullable final DepthAttachment depthAttachement;

    public Framebuffer(
        ResourceLocation pipelineLocation,
        String name,
        List<ColorAttachment> colorAttachements,
        @Nullable DepthAttachment depthAttachement
    ) {
        super(false /* useDepth */);
        this.colorAttachements = colorAttachements;
        this.depthAttachement = depthAttachement;
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
        var firstColor = colorAttachements.size() > 0 ? colorAttachements.get(0) : null;

        Vector3i extent = new Vector3i();
        int lod = 0;
        if (firstColor != null) {
            this.colorTextureId = firstColor.texture.getId();
            extent = firstColor.texture.extent;
            lod = firstColor.lod;
        }
        if (depthAttachement != null) {
            this.depthBufferId = depthAttachement.texture.getId();
            extent = depthAttachement.texture.extent;
            lod = depthAttachement.lod;
        }

        this.viewWidth = extent.x >> lod;
        this.viewHeight = extent.y >> lod;
        this.width = extent.x >> lod;
        this.height = extent.y >> lod;

        this.frameBufferId = GlStateManager.glGenFramebuffers();
        GlStateManager._glBindFramebuffer(GL33C.GL_FRAMEBUFFER, this.frameBufferId);
        if (colorAttachements.size() > 0) {
            GL33C.glDrawBuffers(IntStream.range(0, colorAttachements.size()).map(i -> GL33C.GL_COLOR_ATTACHMENT0+i).toArray());
        }
        else {
            GL33C.glDrawBuffer(GL33C.GL_NONE);
            GL33C.glReadBuffer(GL33C.GL_NONE);
        }

        for (int i = 0; i < colorAttachements.size(); ++i) {
            var a = colorAttachements.get(i);

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

        if (depthAttachement != null) {
            if (depthAttachement.texture.target == GL33C.GL_TEXTURE_2D) {
                GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, depthAttachement.texture.target, depthAttachement.texture.getId(), depthAttachement.lod);
            } else if (depthAttachement.texture.target == GL33C.GL_TEXTURE_2D_ARRAY || depthAttachement.texture.target == GL33C.GL_TEXTURE_3D) {
                GL33C.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, depthAttachement.texture.getId(), depthAttachement.lod, depthAttachement.layer);
            } else {
                throw new NotImplementedException();
            }
        }

        this.checkStatus();
        this.unbindRead();
    }

    /**
     * Called by <code>frex_clear</code>-type passes<p>
     * Note that {@link RenderTarget#clear} clears only first color and depth attachemnts
     */
    public void clearFully() {
        RenderSystem.assertOnRenderThreadOrInit();
        this.bindWrite(true);

        if (this.depthAttachement != null) {
            GlStateManager._clearDepth(depthAttachement.clearDepth);

            if (depthAttachement.lod != 0 && colorAttachements.size() == 0) {
                for (int lod = depthAttachement.lod; lod >= 0; --lod) {
                    GL33C.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, depthAttachement.texture.getId(), lod, depthAttachement.layer);
                    GlStateManager._clear(GL33C.GL_DEPTH_BUFFER_BIT);
                }
            }
            else {
                GlStateManager._clear(GL33C.GL_DEPTH_BUFFER_BIT);
            }
        }

        for (int i = 0; i < this.colorAttachements.size(); ++i) {
            var a = this.colorAttachements.get(i);
            GL33C.glDrawBuffer(GL33C.GL_COLOR_ATTACHMENT0 + i);
            GlStateManager._clearColor(a.clearColor.x, a.clearColor.y, a.clearColor.z, a.clearColor.w);
            GlStateManager._clear(GL33C.GL_COLOR_BUFFER_BIT);
        }

        GL33C.glDrawBuffers(IntStream.range(0, colorAttachements.size()).map(i -> GL33C.GL_COLOR_ATTACHMENT0+i).toArray());
        this.unbindWrite();
    }

}
