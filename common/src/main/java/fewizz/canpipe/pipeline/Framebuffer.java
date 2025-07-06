package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.b3d.GpuDeviceExtended;
import fewizz.canpipe.b3d.GpuTextureExtended;
import fewizz.canpipe.b3d.TextureType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class Framebuffer extends RenderTarget {

    public final String name;

    private final Supplier<List<GpuTextureView>> colorAttachmentsSupplier;
    private final Supplier<GpuTextureView> depthAttachmentSupplier;

    public List<GpuTextureView> colorAttachments;
    public final List<Integer> colorClearColors;
    public @Nullable GpuTextureView depthAttachment;
    public @Nullable final Double depthClearDepth;

    Framebuffer(
        ResourceLocation pipelineLocation,
        String name,
        Supplier<List<GpuTextureView>> colorAttachmentsSupplier,
        List<Integer> colorClearColors,
        Supplier<GpuTextureView> depthAttachmentSupplier,
        @Nullable Double depthClearDepth
    ) {
        super(name, depthClearDepth != null);
        this.name = name;

        this.colorAttachmentsSupplier = colorAttachmentsSupplier;
        this.depthAttachmentSupplier = depthAttachmentSupplier;

        this.colorAttachments = this.colorAttachmentsSupplier.get();
        this.colorClearColors = Collections.unmodifiableList(colorClearColors);
        this.depthAttachment = this.depthAttachmentSupplier.get();
        this.depthClearDepth = depthClearDepth;
        onWindowSizeChanged();
    }

    @Override
    public void resize(int width, int height) {
        // managed by pipeline
    }

    public void onWindowSizeChanged() {
        this.destroyBuffers();
        var window = Minecraft.getInstance().getWindow();
        this.createBuffers(window.getWidth(), window.getHeight());
    }

    @Override
    public void destroyBuffers() {
        if (this.colorAttachments != null) {
            for (var colorAttachment : this.colorAttachments) {
                colorAttachment.close();
            }
            this.colorAttachments = null;
        }

        if (this.depthAttachment != null) {
            this.depthAttachment.close();
            this.depthAttachment = null;
        }

        this.depthTextureView = null;
        this.depthTexture = null;

        this.colorTextureView = null;
        this.colorTexture = null;
    }

    @Override
    public void createBuffers(int width, int height) {
        this.colorAttachments = this.colorAttachmentsSupplier.get();
        this.depthAttachment = depthAttachmentSupplier.get();

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
        this.viewWidth = width;
        this.viewHeight = height;
    }

    static Framebuffer load(
        JsonObject framebufferO,
        ResourceLocation pipelineLocation,
        Function<String, Texture> getOrLoadTexture
    ) {
        String name = framebufferO.get(String.class, "name");
        List<Integer> colorClearColors = new ArrayList<>();

        var colorAttachmentsA = JanksonUtils.listOfObjects(framebufferO, "colorAttachments");

        for (var colorAttachementO : colorAttachmentsA) {
            int clearColor = 0;
            JsonElement clearColorRaw = colorAttachementO.get("clearColor");
            if (clearColorRaw != null) {
                Object clearColorO = ((JsonPrimitive) clearColorRaw).getValue();
                if (clearColorO instanceof Long clearColorL) {
                    clearColor = (int) (long) clearColorL;
                }
                else {
                    throw new NotImplementedException(clearColorO.getClass().getName());
                }
            }
            colorClearColors.add(clearColor);
        }

        Double depthClearDepth = null;
        JsonObject depthAttachmentO = framebufferO.getObject("depthAttachment");

        if (depthAttachmentO != null) {
            depthClearDepth = depthAttachmentO.getDouble("clearDepth", 1.0);
        }

        return new Framebuffer(
            pipelineLocation, name,
            () -> {
                List<GpuTextureView> colorAttachments = new ArrayList<>();

                for (var colorAttachementO : colorAttachmentsA) {
                    int lod = colorAttachementO.getInt("lod", 0);
                    int layer = colorAttachementO.getInt("layer", 0);
                    int face = colorAttachementO.getInt("face", -1);
                    String textureName = colorAttachementO.get(String.class, "image");
                    var texture = getOrLoadTexture.apply(textureName).getTexture();

                    // For cube arrays
                    // (https://registry.khronos.org/vulkan/specs/latest/man/html/VkImageSubresourceRange.html#_description)
                    if (face >= 0) {
                        if (((GpuTextureExtended) texture).canpipe_getType() != TextureType.TYPE_CUBE_MAP) {
                            throw new RuntimeException("Face can be specified only for cube map textures");
                        }
                        layer *= 6;
                        layer += face;
                    }
                    colorAttachments.add(((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                        texture, lod, 1, layer, 1
                    ));
                }
                return colorAttachments;
            },
            colorClearColors,
            () -> {
                GpuTextureView depthAttachment = null;
                if (depthAttachmentO != null) {
                    var lod = Optional.ofNullable(depthAttachmentO.get(Integer.class, "lod"));
                    var layer = Optional.ofNullable(depthAttachmentO.get(Integer.class, "layer"));
                    var texture = getOrLoadTexture.apply(depthAttachmentO.get(String.class, "image")).getTexture();
                    depthAttachment = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                        texture, lod.orElse(0), 1, layer.orElse(0), 1
                    );
                }
                return depthAttachment;
            }, depthClearDepth
        );
    }

}
