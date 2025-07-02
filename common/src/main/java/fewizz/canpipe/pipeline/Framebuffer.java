package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL33C;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import fewizz.canpipe.GFX;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.b3d.GlDebugLabelExtended;
import fewizz.canpipe.b3d.GpuTextureExtended;
import fewizz.canpipe.b3d.TextureType;
import net.minecraft.resources.ResourceLocation;

public class Framebuffer extends RenderTarget implements AutoCloseable {

    public static record ColorAttachment(
        GpuTextureView textureView,
        Vector4f clearColor,
        int lod,
        int layer,
        int face
    ) {}

    public static record DepthAttachment(
        GpuTextureView textureView,
        double clearDepth,
        Optional<Integer> lod,
        Optional<Integer> layer
    ) {}

    public final List<ColorAttachment> colorAttachments;
    public final @Nullable DepthAttachment depthAttachment;
    public final String name;
    private int id;

    Framebuffer(
        ResourceLocation pipelineLocation,
        String name,
        List<ColorAttachment> colorAttachments,
        @Nullable DepthAttachment depthAttachment
    ) {
        super(name, depthAttachment != null);
        this.name = name;
        this.colorAttachments = Collections.unmodifiableList(colorAttachments);
        this.depthAttachment = depthAttachment;
        this.createBuffers(-1, -1);

        if (
            RenderSystem.getDevice() instanceof GlDevice glDevice &&
            glDevice.debugLabels() instanceof GlDebugLabelExtended labels
        ) {
            labels.canpipe_applyLabelFramebuffer(this.id, this.name);
        }
    }

    public int glID() {
        return this.id;
    }

    @Override
    public void destroyBuffers() {
        RenderSystem.assertOnRenderThread();
        this.close();
    }

    @Override
    public void close() {
        if (this.id > -1) {
            GlStateManager._glBindFramebuffer(36160, 0);
            GlStateManager._glDeleteFramebuffers(this.id);
            this.id = -1;
        }
    }

    @Override
    public void createBuffers(int width, int height) {
        GpuDevice gpuDevice = RenderSystem.getDevice();
        Vector3i extent = new Vector3i();
        int lod = 0;

        if (this.colorAttachments.size() > 0) {
            var firstColor = this.colorAttachments.get(0);
            this.colorTexture = firstColor.textureView.texture();
            // this.colorTexture.setAddressMode(AddressMode.CLAMP_TO_EDGE);
            // this.colorTexture.setTextureFilter(FilterMode.NEAREST, true);
            extent.x = Math.max(extent.x, firstColor.textureView.getWidth(0));
            extent.y = Math.max(extent.y, firstColor.textureView.getHeight(0));
            lod = Math.max(lod, firstColor.lod);
        }
        if (this.depthAttachment != null) {
            var texture = this.depthAttachment.textureView().texture();
            this.depthTexture = new GlTexture(
                GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                this.name, texture.getFormat(),
                texture.getWidth(0), texture.getHeight(0),
                1, // texture.getDepthOrLayers(),
                texture.getMipLevels(), ((GlTexture)texture).glId()
            ) {

                @Override public void close() {}
                @Override public boolean isClosed() { return false; }
                @Override public void flushModeChanges(int target) {}

                @Override
                public int getFbo(DirectStateAccess directStateAccess, @Nullable GpuTexture gpuTexture) {
                    return glID();
                }
            };
            // this.depthTexture.setTextureFilter(FilterMode.NEAREST, false);
            //this.depthTexture.setAddressMode(AddressMode.CLAMP_TO_EDGE);
            extent.x = Math.max(extent.x, depthAttachment.textureView.getWidth(0));
            extent.y = Math.max(extent.y, depthAttachment.textureView.getHeight(0));
            lod = Math.max(lod, this.depthAttachment.lod.orElse(0));
        }

        // TODO: what's the difference?
        this.viewWidth = extent.x >> lod;
        this.viewHeight = extent.y >> lod;
        this.width = extent.x >> lod;
        this.height = extent.y >> lod;

        {
            var texture = this.colorTexture != null ? this.colorTexture : this.depthTexture;
            this.colorTexture = new GlTexture(
                GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                this.name, texture.getFormat(),
                texture.getWidth(0), texture.getHeight(0),
                1, // texture.getDepthOrLayers(),
                texture.getMipLevels(), ((GlTexture)texture).glId()
            ) {

                @Override public void close() {}
                @Override public boolean isClosed() { return false; }
                @Override public void flushModeChanges(int target) {}

                @Override
                public int getFbo(DirectStateAccess directStateAccess, @Nullable GpuTexture gpuTexture) {
                    return glID();
                }
            };
        }

        if (this.colorTexture != null) {
            this.colorTextureView = gpuDevice.createTextureView(this.colorTexture);
        }
        if (this.depthTexture != null) {
            this.depthTextureView = gpuDevice.createTextureView(this.depthTexture);
        }

        this.id = GlStateManager.glGenFramebuffers();
        GlStateManager._glBindFramebuffer(GL33C.GL_FRAMEBUFFER, this.id);
        GFX.glDrawBuffers(IntStream.range(0, colorAttachments.size()).map(i -> GL33C.GL_COLOR_ATTACHMENT0+i).toArray());

        for (int attachmentIndex = 0; attachmentIndex < colorAttachments.size(); ++attachmentIndex) {
            var attachment = colorAttachments.get(attachmentIndex);

            var textureType = ((GpuTextureExtended) attachment.textureView.texture()).canpipe_getType();
            var textureID = ((GlTextureView) attachment.textureView).texture().glId();

            if (textureType == TextureType.TYPE_2D) {
                GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + attachmentIndex, GL33C.GL_TEXTURE_2D, textureID, attachment.lod);
            } else if (textureType == TextureType.TYPE_2D) {
                GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + attachmentIndex, textureID, attachment.lod, attachment.layer);
            } else if (textureType == TextureType.TYPE_CUBE_MAP) {
                int face = attachment.face != -1 ? attachment.face : attachment.layer;  // for compatibility
                GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + attachmentIndex, GL33C.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, textureID, attachment.lod);
            } /* else if (attachment.textureView.target == GL40C.GL_TEXTURE_CUBE_MAP_ARRAY) {
                GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0 + attachmentIndex, textureID, attachment.lod, attachment.layer * 6 + attachment.face);
            } */ else {
                throw new NotImplementedException();
            }
        }

        if (this.depthAttachment != null) {
            var textureType = ((GpuTextureExtended) this.depthAttachment.textureView.texture()).canpipe_getType();
            var textureID = ((GlTextureView) this.depthAttachment.textureView).texture().glId();

            if (textureType == TextureType.TYPE_2D) {
                GlStateManager._glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, GL33C.GL_TEXTURE_2D, textureID, this.depthAttachment.lod.orElse(0));
            } else if (textureType == TextureType.TYPE_2D_ARRAY) {
                GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, textureID, this.depthAttachment.lod.orElse(0), this.depthAttachment.layer.orElse(0));
            } else {
                throw new NotImplementedException();
            }
        }

        GlStateManager._glBindFramebuffer(GL33C.GL_FRAMEBUFFER, 0);
    }

    /**
     * Called by <code>frex_clear</code>-type passes<p>
     * Note that {@link RenderTarget#clear} clears only first color and depth attachemnts
     */
    public void bindAndClearFully() {
        RenderSystem.assertOnRenderThread();
        GlStateManager._glBindFramebuffer(GL33C.GL_FRAMEBUFFER, this.id);

        if (this.depthAttachment != null) {
            GFX.glClearDepth(this.depthAttachment.clearDepth);
            GpuTexture depthTexture = this.depthAttachment.textureView.texture();
            TextureType textureType = ((GpuTextureExtended) this.depthAttachment.textureView.texture()).canpipe_getType();
            var textureID = ((GlTextureView) this.depthAttachment.textureView).texture().glId();

            if (textureType == TextureType.TYPE_2D_ARRAY) {
                for (int lod = this.depthAttachment.lod.orElse(depthTexture.getMipLevels()-1); lod >= this.depthAttachment.lod.orElse(0); --lod) {
                    for (int layer = this.depthAttachment.layer.orElse(depthTexture.getDepthOrLayers()-1); layer >= this.depthAttachment.layer.orElse(0); --layer) {
                        GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, textureID, lod, layer);
                        GlStateManager._depthMask(true);
                        GlStateManager._clear(GL33C.GL_DEPTH_BUFFER_BIT);
                    }
                }
            }
            else {
                GlStateManager._depthMask(true);
                GlStateManager._clear(GL33C.GL_DEPTH_BUFFER_BIT);
            }
        }

        for (int i = 0; i < this.colorAttachments.size(); ++i) {
            var a = this.colorAttachments.get(i);
            GFX.glDrawBuffers(new int[] {GL33C.GL_COLOR_ATTACHMENT0 + i});
            GFX.glClearColor(a.clearColor.x, a.clearColor.y, a.clearColor.z, a.clearColor.w);
            GlStateManager._clear(GL33C.GL_COLOR_BUFFER_BIT);
        }

        GFX.glDrawBuffers(IntStream.range(0, colorAttachments.size()).map(i -> GL33C.GL_COLOR_ATTACHMENT0+i).toArray());
    }

    static Framebuffer load(
        JsonObject framebufferO,
        ResourceLocation pipelineLocation,
        Function<String, GpuTextureView> getOrLoadTextureView
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

            var texture = getOrLoadTextureView.apply(textureName);
            colorAttachements.add(new Framebuffer.ColorAttachment(texture, clearColor, lod, layer, face));
        }

        Framebuffer.DepthAttachment depthAttachement = null;
        JsonObject depthAttachementO = framebufferO.getObject("depthAttachment");

        if (depthAttachementO != null) {
            var texture = getOrLoadTextureView.apply(depthAttachementO.get(String.class, "image"));
            var lod = Optional.ofNullable(depthAttachementO.get(Integer.class, "lod"));
            var layer = Optional.ofNullable(depthAttachementO.get(Integer.class, "layer"));

            double clearDepth = depthAttachementO.getDouble("clearDepth", 1.0);
            depthAttachement = new Framebuffer.DepthAttachment(texture, clearDepth, lod, layer);
        }

        return new Framebuffer(pipelineLocation, name, colorAttachements, depthAttachement);
    }

}
