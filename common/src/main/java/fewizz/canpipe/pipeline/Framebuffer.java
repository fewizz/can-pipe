package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.b3d.GpuDeviceExtended;
import fewizz.canpipe.b3d.GpuTextureExtended;
import fewizz.canpipe.b3d.TextureType;
import net.minecraft.resources.ResourceLocation;

public class Framebuffer extends RenderTarget {

    public static record ColorAttachment(
        GpuTextureView textureView,
        Vector4f clearColor,
        int lod,
        int layer
    ) {}

    public static record DepthAttachment(
        GpuTextureView textureView,
        double clearDepth,
        Optional<Integer> lod,
        Optional<Integer> layer
    ) {}

    public final List<GpuTextureView> colorAttachments;
    public final List<Vector4f> colorClearColors;
    public final @Nullable GpuTextureView depthAttachment;
    public final @Nullable Double depthClearDepth;
    public final String name;

    Framebuffer(
        ResourceLocation pipelineLocation,
        String name,
        List<GpuTextureView> colorAttachments,
        List<Vector4f> colorClearColors,
        GpuTextureView depthAttachment,
        Double depthClearDepth
    ) {
        super(name, depthAttachment != null);
        this.name = name;
        this.colorAttachments = Collections.unmodifiableList(colorAttachments);
        this.colorClearColors = Collections.unmodifiableList(colorClearColors);
        this.depthAttachment = depthAttachment;
        this.depthClearDepth = depthClearDepth;
        this.createBuffers(-1, -1);
    }

    @Override
    public void destroyBuffers() {}

    @Override
    public void createBuffers(int width, int height) {
        if (this.colorAttachments.size() > 0) {
            this.colorTextureView = this.colorAttachments.get(0);
            this.colorTexture = this.colorTextureView.texture();
            width = Math.max(width, this.colorTextureView.getWidth(0));
            height = Math.max(height, this.colorTextureView.getHeight(0));
        }
        if (this.depthAttachment != null) {
            this.depthTextureView = this.depthAttachment;
            this.depthTexture = this.depthTextureView.texture();
            width = Math.max(width, this.depthTextureView.getWidth(0));
            height = Math.max(height, this.depthTextureView.getHeight(0));
        }

        this.width = width;
        this.height = height;
        this.viewWidth = this.width;
        this.viewHeight = this.height;
    }

    static Framebuffer load(
        JsonObject framebufferO,
        ResourceLocation pipelineLocation,
        Function<String, GpuTexture> getOrLoadTexture
    ) {
        String name = framebufferO.get(String.class, "name");
        List<Framebuffer.ColorAttachment> colorAttachments = new ArrayList<>();

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

            // For cube arrays
            // (https://registry.khronos.org/vulkan/specs/latest/man/html/VkImageSubresourceRange.html#_description)
            if (face >= 0) {
                if (((GpuTextureExtended) texture).canpipe_getType() != TextureType.TYPE_CUBE_MAP) {
                    throw new RuntimeException("Face can be specified only for cube map textures");
                }
                layer *= 6;
                layer += face;
            }

            var textureView = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                texture, lod, 1, layer, 1
            );
            colorAttachments.add(new Framebuffer.ColorAttachment(textureView, clearColor, lod, layer));
        }

        Framebuffer.DepthAttachment depthAttachment = null;
        JsonObject depthAttachmentO = framebufferO.getObject("depthAttachment");

        if (depthAttachmentO != null) {
            var lod = Optional.ofNullable(depthAttachmentO.get(Integer.class, "lod"));
            var layer = Optional.ofNullable(depthAttachmentO.get(Integer.class, "layer"));

            var texture = getOrLoadTexture.apply(depthAttachmentO.get(String.class, "image"));
            var textureView = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                texture, lod.orElse(0), 1, layer.orElse(0), 1
            );

            double clearDepth = depthAttachmentO.getDouble("clearDepth", 1.0);
            depthAttachment = new Framebuffer.DepthAttachment(textureView, clearDepth, lod, layer);
        }

        return new Framebuffer(
            pipelineLocation,
            name,
            colorAttachments.stream().map(a -> a.textureView()).toList(),
            colorAttachments.stream().map(a -> a.clearColor()).toList(),
            depthAttachment != null ? depthAttachment.textureView() : null,
            depthAttachment != null ? depthAttachment.clearDepth() : null
        );
    }

}
