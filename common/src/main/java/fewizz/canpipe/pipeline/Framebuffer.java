package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.b3d.GpuDeviceExtended;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class Framebuffer extends RenderTarget {

    public final String name;

    private final Supplier<List<Pair<GpuTexture, GpuTextureView>>> colorAttachmentsSupplier;
    private final Supplier<Pair<GpuTexture, GpuTextureView>> depthAttachmentSupplier;

    public final List<GpuTextureView> colorAttachments;
    public final List<GpuTexture> colorAttachmentTextures;  // neoforge: GlTextureView.texture points to GlTexture, but we need ValidationGpuTexture
    public final List<Integer> colorClearColors;

    public @Nullable final Double depthClearDepth;

    Framebuffer(
        ResourceLocation pipelineLocation,
        String name,
        Supplier<List<Pair<GpuTexture, GpuTextureView>>> colorAttachmentsSupplier,
        List<Integer> colorClearColors,
        Supplier<Pair<GpuTexture, GpuTextureView>> depthAttachmentSupplier,
        @Nullable Double depthClearDepth
    ) {
        super(name, depthClearDepth != null);
        this.name = name;

        this.colorAttachmentsSupplier = colorAttachmentsSupplier;
        this.depthAttachmentSupplier = depthAttachmentSupplier;

        this.colorAttachments = new ArrayList<>();
        this.colorAttachmentTextures = new ArrayList<>();
        this.colorClearColors = Collections.unmodifiableList(colorClearColors);

        this.depthClearDepth = depthClearDepth;

        var window = Minecraft.getInstance().getWindow();
        this.createBuffers(window.getWidth(), window.getHeight());
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
        for (var colorAttachment : this.colorAttachments) {
            colorAttachment.close();
        }

        if (this.depthTextureView != null) {
            this.depthTextureView.close();
            this.depthTextureView = null;
        }

        this.depthTextureView = null;
        this.depthTexture = null;

        this.colorTextureView = null;
        this.colorTexture = null;
    }

    @Override
    public void createBuffers(int width, int height) {
        this.colorAttachmentTextures.clear();
        this.colorAttachments.clear();
        for (var t : this.colorAttachmentsSupplier.get()) {
            this.colorAttachmentTextures.add(t.getLeft());
            this.colorAttachments.add(t.getRight());
        }

        var t = this.depthAttachmentSupplier.get();
        this.depthTexture = t.getLeft();
        this.depthTextureView = t.getRight();

        if (this.colorAttachments.size() > 0) {
            this.colorTextureView = this.colorAttachments.get(0);
            this.colorTexture = this.colorAttachmentTextures.get(0);
            width = Math.max(width, this.colorTextureView.getWidth(0));
            height = Math.max(height, this.colorTextureView.getHeight(0));
        }
        if (this.depthTextureView != null) {
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
                List<Pair<GpuTexture, GpuTextureView>> colorAttachments = new ArrayList<>();

                for (var colorAttachementO : colorAttachmentsA) {
                    int lod = colorAttachementO.getInt("lod", 0);
                    int layer = colorAttachementO.getInt("layer", 0);
                    int face = colorAttachementO.getInt("face", -1);
                    String textureName = colorAttachementO.get(String.class, "image");
                    var texture = getOrLoadTexture.apply(textureName).getTexture();

                    // For cube arrays
                    // (https://registry.khronos.org/vulkan/specs/latest/man/html/VkImageSubresourceRange.html#_description)
                    if (face >= 0) {
                        if ((texture.usage() & GpuTexture.USAGE_CUBEMAP_COMPATIBLE) == 0) {
                            throw new RuntimeException("Face can be specified only for cube map textures");
                        }
                        layer *= 6;
                        layer += face;
                    }
                    var textureView = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                        texture, lod, 1, layer, 1
                    );
                    colorAttachments.add(Pair.of(texture, textureView));
                }
                return colorAttachments;
            },
            colorClearColors,
            () -> {
                GpuTextureView depthAttachment = null;
                GpuTexture texture = null;
                if (depthAttachmentO != null) {
                    var lod = Optional.ofNullable(depthAttachmentO.get(Integer.class, "lod"));
                    var layer = Optional.ofNullable(depthAttachmentO.get(Integer.class, "layer"));
                    texture = getOrLoadTexture.apply(depthAttachmentO.get(String.class, "image")).getTexture();
                    depthAttachment = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                        texture, lod.orElse(0), 1, layer.orElse(0), 1
                    );
                }
                return Pair.of(texture, depthAttachment);
            }, depthClearDepth
        );
    }

}
