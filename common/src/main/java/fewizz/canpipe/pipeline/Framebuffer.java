package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.commons.lang3.NotImplementedException;
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
        Texture texture,
        Vector4f clearColor,
        int lod,
        int layer,
        int face
    ) {}

    public static record DepthAttachment(
        Texture texture,
        double clearDepth,
        Optional<Integer> lod,
        Optional<Integer> layer
    ) {}

    public final List<ColorAttachment> colorAttachments;
    public final @Nullable DepthAttachment depthAttachment;
    public final String name;

    private Framebuffer(
        ResourceLocation pipelineLocation,
        String name,
        List<ColorAttachment> colorAttachments,
        @Nullable DepthAttachment depthAttachment
    ) {
        super(depthAttachment != null);
        this.name = name;
        this.colorAttachments = colorAttachments;
        this.depthAttachment = depthAttachment;
        createBuffers(0, 0);
        GFX.glObjectLabel(GL33C.GL_FRAMEBUFFER, this.frameBufferId, pipelineLocation.toString()+"-"+name);
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
    public void createBuffers(int width, int height) {
        Vector3i extent = new Vector3i();
        int lod = 0;

        if (this.colorAttachments.size() > 0) {
            var firstColor = this.colorAttachments.get(0);
            this.colorTextureId = firstColor.texture.getId();
            extent.max(firstColor.texture.extent);
            lod = Math.max(lod, firstColor.lod);
        }
        if (this.depthAttachment != null) {
            this.depthBufferId = this.depthAttachment.texture.getId();
            extent.max(this.depthAttachment.texture.extent);
            lod = Math.max(lod, this.depthAttachment.lod.orElse(0));
        }

        // TODO: what's the difference?
        this.viewWidth = extent.x >> lod;
        this.viewHeight = extent.y >> lod;
        this.width = extent.x >> lod;
        this.height = extent.y >> lod;

        this.frameBufferId = GlStateManager.glGenFramebuffers();
        GlStateManager._glBindFramebuffer(GL33C.GL_FRAMEBUFFER, this.frameBufferId);
        GFX.glDrawBuffers(IntStream.range(0, colorAttachments.size()).map(i -> GL33C.GL_COLOR_ATTACHMENT0+i).toArray());

        for (int attachmentIndex = 0; attachmentIndex < colorAttachments.size(); ++attachmentIndex) {
            var attachment = colorAttachments.get(attachmentIndex);

            if (attachment.texture.target == GL33C.GL_TEXTURE_2D) {
                GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + attachmentIndex, attachment.texture.target, attachment.texture.getId(), attachment.lod);
            } else if (attachment.texture.target == GL33C.GL_TEXTURE_2D_ARRAY || attachment.texture.target == GL33C.GL_TEXTURE_3D) {
                GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + attachmentIndex, attachment.texture.getId(), attachment.lod, attachment.layer);
            } else if (attachment.texture.target == GL33C.GL_TEXTURE_CUBE_MAP) {
                int face = attachment.face != -1 ? attachment.face : attachment.layer;  // for compatibility
                GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + attachmentIndex, GL33C.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, attachment.texture.getId(), attachment.lod);
            } else if (attachment.texture.target == GL40C.GL_TEXTURE_CUBE_MAP_ARRAY) {
                GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + attachmentIndex, attachment.texture.getId(), attachment.lod, attachment.layer * 6 + attachment.face);
            } else {
                throw new NotImplementedException();
            }
        }

        if (this.depthAttachment != null) {
            if (this.depthAttachment.texture.target == GL33C.GL_TEXTURE_2D) {
                GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, this.depthAttachment.texture.target, this.depthAttachment.texture.getId(), this.depthAttachment.lod.orElse(0));
            } else if (this.depthAttachment.texture.target == GL33C.GL_TEXTURE_2D_ARRAY || this.depthAttachment.texture.target == GL33C.GL_TEXTURE_3D) {
                GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, this.depthAttachment.texture.getId(), this.depthAttachment.lod.orElse(0), this.depthAttachment.layer.orElse(0));
            } else {
                throw new NotImplementedException();
            }
        }

        try {
            this.checkStatus();
        }
        catch (Exception e) {
            this.close();
            throw new RuntimeException("Error occurred while trying to initialize framebuffer \""+name+"\": " + e.getMessage(), e);
        }
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
            GlStateManager._clearDepth(this.depthAttachment.clearDepth);
            Texture depthTexture = this.depthAttachment.texture;

            if (depthTexture.target == GL33C.GL_TEXTURE_2D_ARRAY || depthTexture.target == GL33C.GL_TEXTURE_3D) {
                for (int lod = this.depthAttachment.lod.orElse(depthTexture.maxLod); lod >= this.depthAttachment.lod.orElse(0); --lod) {
                    for (int layer = this.depthAttachment.layer.orElse(depthTexture.extent.z-1); layer >= this.depthAttachment.layer.orElse(0); --layer) {
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
                    clearColor.set(
                        (l >> 24) & 0xFF,
                        (l >> 16) & 0xFF,
                        (l >> 8 ) & 0xFF,
                        (l >> 0 ) & 0xFF
                    ).div(255.0F);
                }
                else {
                    throw new NotImplementedException(clearColorO.getClass().getName());
                }
            }

            var texture = getOrLoadTexture.apply(textureName);
            colorAttachements.add(new Framebuffer.ColorAttachment(texture, clearColor, lod, layer, face));
        }

        Framebuffer.DepthAttachment depthAttachement = null;
        JsonObject depthAttachementO = framebufferO.getObject("depthAttachment");

        if (depthAttachementO != null) {
            var texture = getOrLoadTexture.apply(depthAttachementO.get(String.class, "image"));
            var lod = Optional.ofNullable(depthAttachementO.get(Integer.class, "lod"));
            var layer = Optional.ofNullable(depthAttachementO.get(Integer.class, "layer"));

            double clearDepth = depthAttachementO.getDouble("clearDepth", 1.0);
            depthAttachement = new Framebuffer.DepthAttachment(texture,clearDepth, lod, layer);
        }

        return new Framebuffer(pipelineLocation, name, colorAttachements, depthAttachement);
    }

}
