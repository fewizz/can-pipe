package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL40C;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import fewizz.canpipe.GFX;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.resources.ResourceLocation;

public class Framebuffer extends RenderTarget implements AutoCloseable {

    public static record ColorAttachment(
        @NotNull Texture texture,
        @NotNull Vector4f clearColor,
        int lod,
        int layer,
        int face
    ) {}

    public static record DepthAttachment(
        @NotNull Texture texture,
        @NotNull double clearDepth,
        Optional<Integer> lod,
        Optional<Integer> layer
    ) {}

    public final List<ColorAttachment> colorAttachments;
    public final @Nullable DepthAttachment depthAttachment;

    private Framebuffer(
        ResourceLocation pipelineLocation,
        String name,
        List<ColorAttachment> colorAttachments,
        @Nullable DepthAttachment depthAttachment
    ) {
        super(false /* useDepth */);
        this.colorAttachments = colorAttachments;
        this.depthAttachment = depthAttachment;
        createBuffers(0, 0);
        GFX.glObjectLabel(
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

        this.close();
    }

    @Override
    public void close() {
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
            GFX.glDrawBuffers(IntStream.range(0, colorAttachments.size()).map(i -> GL33C.GL_COLOR_ATTACHMENT0+i).toArray());
        }
        else {
            GFX.glDrawBuffers(new int[]{});
        }

        for (int i = 0; i < colorAttachments.size(); ++i) {
            var a = colorAttachments.get(i);

            if (a.texture.target == GL33C.GL_TEXTURE_2D) {
                GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + i, a.texture.target, a.texture.getId(), a.lod);
            } else if (a.texture.target == GL33C.GL_TEXTURE_2D_ARRAY || a.texture.target == GL33C.GL_TEXTURE_3D) {
                GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + i, a.texture.getId(), a.lod, a.layer);
            } else if (a.texture.target == GL33C.GL_TEXTURE_CUBE_MAP) {
                int face = a.face != -1 ? a.face : a.layer; // for compatibility
                GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + i, GL33C.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, a.texture.getId(), a.lod);
            } else if (a.texture.target == GL40C.GL_TEXTURE_CUBE_MAP_ARRAY) {
                GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + i, a.texture.getId(), a.lod, a.layer * 6 + a.face);
            } else {
                throw new NotImplementedException();
            }
        }

        if (depthAttachment != null) {
            if (depthAttachment.texture.target == GL33C.GL_TEXTURE_2D) {
                GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, depthAttachment.texture.target, depthAttachment.texture.getId(), depthAttachment.lod.orElse(0));
            } else if (depthAttachment.texture.target == GL33C.GL_TEXTURE_2D_ARRAY || depthAttachment.texture.target == GL33C.GL_TEXTURE_3D) {
                GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, depthAttachment.texture.getId(), depthAttachment.lod.orElse(0), depthAttachment.layer.orElse(0));
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

        GFX.glDrawBuffers(new int[] {GL33C.GL_COLOR_ATTACHMENT0 });

        super.clear();

        this.bindWrite(false);
        GFX.glDrawBuffers(IntStream.range(0, colorAttachments.size()).map(i -> GL33C.GL_COLOR_ATTACHMENT0+i).toArray());
        this.unbindWrite();
    }

    /**
     * Called by <code>frex_clear</code>-type passes<p>
     * Note that {@link RenderTarget#clear} clears only first color and depth attachemnts
     */
    public void bindAndClearFully() {
        RenderSystem.assertOnRenderThreadOrInit();
        this.bindWrite(true);

        if (this.depthAttachment != null) {
            GlStateManager._clearDepth(depthAttachment.clearDepth);
            Texture depthTexture = depthAttachment.texture;

            if (depthTexture.target == GL33C.GL_TEXTURE_2D_ARRAY || depthTexture.target == GL33C.GL_TEXTURE_3D) {
                for (int lod = depthAttachment.lod.orElse(depthTexture.maxLod); lod >= depthAttachment.lod.orElse(0); --lod) {
                    for (int layer = depthAttachment.layer.orElse(depthTexture.extent.z-1); layer >= depthAttachment.layer.orElse(0); --layer) {
                        GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, depthTexture.getId(), lod, layer);
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
            GFX.glDrawBuffers(new int[] {GL33C.GL_COLOR_ATTACHMENT0 + i});
            GlStateManager._clearColor(a.clearColor.x, a.clearColor.y, a.clearColor.z, a.clearColor.w);
            GlStateManager._clear(GL33C.GL_COLOR_BUFFER_BIT);
        }

        GFX.glDrawBuffers(IntStream.range(0, colorAttachments.size()).map(i -> GL33C.GL_COLOR_ATTACHMENT0+i).toArray());
    }

    static Framebuffer load(
        JsonObject framebufferO,
        ResourceLocation pipelineLocation,
        Function<String, Texture> getOrLoadTexture
    ) {
        String name = framebufferO.get(String.class, "name");
        List<Framebuffer.ColorAttachment> colorAttachements = new ArrayList<>();

        for (var colorAttachementO : JanksonUtils.listOfObjects(framebufferO, "colorAttachments")) {
            String textureName = colorAttachementO.get(String.class, "image");
            int lod = colorAttachementO.getInt("lod", 0);
            int layer = colorAttachementO.getInt("layer", 0);
            int face = colorAttachementO.getInt("face", -1);

            Vector4f clearColor = new Vector4f(0.0F);
            JsonElement clearColorRaw = colorAttachementO.get("clearColor");
            if (clearColorRaw != null) {
                Object clearColorO = ((JsonPrimitive) clearColorRaw).getValue();
                if (clearColorO instanceof Long l) {
                    clearColor.x = ((l >> 24) & 0xFF) / 255f;
                    clearColor.y = ((l >> 16) & 0xFF) / 255f;
                    clearColor.z = ((l >> 8)  & 0xFF) / 255f;
                    clearColor.w = ((l >> 0)  & 0xFF) / 255f;
                }
                else {
                    throw new NotImplementedException(clearColorO.getClass().getName());
                }
            }

            colorAttachements.add(new Framebuffer.ColorAttachment(
                getOrLoadTexture.apply(textureName),
                clearColor, lod, layer, face
            ));
        }
        Framebuffer.DepthAttachment depthAttachement = null;
        JsonObject depthAttachementO = framebufferO.getObject("depthAttachment");
        if (depthAttachementO != null) {
            var depthTexture = getOrLoadTexture.apply(depthAttachementO.get(String.class, "image"));

            double clearDepth = depthAttachementO.getDouble("clearDepth", 1.0);
            depthAttachement = new Framebuffer.DepthAttachment(
                depthTexture,
                clearDepth,
                Optional.ofNullable(depthAttachementO.get(Integer.class, "lod")),
                Optional.ofNullable(depthAttachementO.get(Integer.class, "layer"))
            );
        }
        return new Framebuffer(pipelineLocation, name, colorAttachements, depthAttachement);
    }

}
