package fewizz.canpipe.pipeline;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
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
import net.minecraft.resources.Identifier;

public class Framebuffer extends RenderTarget {

    public final String name;

    private final IntFunction<Pair<GpuTexture, GpuTextureView>> colorAttachmentsSupplier;
    private final Supplier<Pair<GpuTexture, GpuTextureView>> depthAttachmentSupplier;

    public final GpuTexture[] colorTextures;
    public final GpuTextureView[] colorTextureViews;
    public final int[] colorTextureClearColors;

    public @Nullable final Double depthTextureClearDepth;

    private boolean destroyed = true;

    Framebuffer(
        Identifier pipelineLocation,
        String name,
        IntFunction<Pair<GpuTexture, GpuTextureView>> colorTextureSupplier,
        int[] colorClearColors,
        Supplier<Pair<GpuTexture, GpuTextureView>> depthTextureSupplier,
        @Nullable Double depthClearDepth
    ) {
        super(name, depthClearDepth != null, null /* don't care about color's `this.format` */);

        this.name = name;
        this.colorAttachmentsSupplier = colorTextureSupplier;
        this.depthAttachmentSupplier = depthTextureSupplier;
        this.colorTextures = new GpuTexture[colorClearColors.length];
        this.colorTextureViews = new GpuTextureView[colorClearColors.length];
        this.colorTextureClearColors = colorClearColors;
        this.depthTextureClearDepth = depthClearDepth;

        this.createBuffers(-1, -1);
    }

    @Override
    public void resize(int width, int height) {
        // managed by pipeline
    }

    public void onWindowSizeChanged() {
        this.destroyBuffers();
        this.createBuffers(-1, -1);
    }

    @Override
    public void destroyBuffers() {
        if (destroyed) {
            return;
        }

        for (int i = 0; i < this.colorTextures.length; ++i) {
            this.colorTextureViews[i].close();
            this.colorTextureViews[i] = null;
        }

        if (this.depthTextureView != null) {
            this.depthTextureView.close();
            this.depthTextureView = null;
        }

        this.depthTextureView = null;
        this.depthTexture = null;

        this.colorTextureView = null;
        this.colorTexture = null;

        destroyed = true;
    }

    @Override
    public void createBuffers(int width, int height) {
        if (!destroyed) { throw new RuntimeException(); }

        for (int i = 0; i < this.colorTextures.length; ++i) {
            var colorTextureAndView = this.colorAttachmentsSupplier.apply(i);
            var texture = colorTextureAndView.getLeft();
            var textureView = colorTextureAndView.getRight();

            if (texture == null) { throw new RuntimeException("Color attachment supplier must not return null for a texture"); }
            if (textureView == null) { throw new RuntimeException("Color attachment supplier must not return null for a texture view"); }

            this.colorTextures[i] = texture;
            this.colorTextureViews[i] = textureView;
        }

        if (this.colorTextures.length > 0) {
            this.colorTexture = this.colorTextures[0];
            this.colorTextureView = this.colorTextureViews[0];
        }

        var depthTextureAndView = this.depthAttachmentSupplier.get();
        this.depthTexture = depthTextureAndView.getLeft();
        this.depthTextureView = depthTextureAndView.getRight();

        this.width = this.height = -1;

        Consumer<GpuTextureView> updateAndValidateSize = (textureView) -> {
            int w = textureView.getWidth(0);
            int h = textureView.getHeight(0);

            if (this.width == -1) {
                this.width = w;
                this.height = h;
            }
            else {
                if (w != this.width) {
                    throw new RuntimeException("Expected texture view width="+this.width+", but got width="+w);
                }
                if (h != this.height) {
                    throw new RuntimeException("Expected texture view height="+this.height+", but got height="+h);
                }
            }
        };

        for (var textureView : this.colorTextureViews) {
            updateAndValidateSize.accept(textureView);
        }
        if (this.depthTextureView != null) {
            updateAndValidateSize.accept(this.depthTextureView);
        }

        destroyed = false;
    }

    static Framebuffer load(
        JsonObject framebufferJson,
        Identifier pipelineLocation,
        Function<String, Texture> getOrLoadTexture
    ) {
        String name = framebufferJson.get(String.class, "name");
        var colorAttachmentJsons = JanksonUtils.listOfObjects(framebufferJson, "colorAttachments");

        int[] colorTextureClearColors = new int[colorAttachmentJsons.size()];

        for (int i = 0; i < colorTextureClearColors.length; ++i) {
            int clearColor = 0x00000000;
            JsonElement clearColorJson = colorAttachmentJsons.get(i).get("clearColor");
            if (clearColorJson != null) {
                Object clearColorRaw = ((JsonPrimitive) clearColorJson).getValue();
                if (clearColorRaw instanceof Long clearColorL) {
                    clearColor = (int) (long) clearColorL;
                }
                else {
                    throw new NotImplementedException(clearColorRaw.getClass().getName());
                }
            }
            colorTextureClearColors[i] = clearColor;
        }

        Double depthClearDepth = null;
        JsonObject depthAttachmentJson = framebufferJson.getObject("depthAttachment");

        if (depthAttachmentJson != null) {
            depthClearDepth = depthAttachmentJson.getDouble("clearDepth", 1.0);
        }

        return new Framebuffer(
            pipelineLocation, name,
            (int idx) -> {
                JsonObject colorAttachmentJson = colorAttachmentJsons.get(idx);
                int lod = colorAttachmentJson.getInt("lod", 0);
                int baseLayer = colorAttachmentJson.getInt("layer", 0);
                int layerCount = 1;
                int face = colorAttachmentJson.getInt("face", -1);
                String textureName = colorAttachmentJson.get(String.class, "image");
                GpuTexture texture = getOrLoadTexture.apply(textureName).getTexture();

                boolean cubemap = (texture.usage() & GpuTexture.USAGE_CUBEMAP_COMPATIBLE) != 0;

                if (face != -1) {
                    if (!cubemap) {
                        throw new RuntimeException("Face can be specified only for cube map textures");
                    }
                    baseLayer = baseLayer * 6 + face;
                }

                var textureView = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                    texture, lod, 1, baseLayer, layerCount
                );
                return Pair.of(texture, textureView);
            },
            colorTextureClearColors,
            () -> {
                GpuTexture texture = null;
                GpuTextureView textureView = null;
                if (depthAttachmentJson != null) {
                    var lod = Optional.ofNullable(depthAttachmentJson.get(Integer.class, "lod"));
                    var layer = Optional.ofNullable(depthAttachmentJson.get(Integer.class, "layer"));
                    texture = getOrLoadTexture.apply(depthAttachmentJson.get(String.class, "image")).getTexture();
                    textureView = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                        texture, lod.orElse(0), 1, layer.orElse(0), 1
                    );
                }
                return Pair.of(texture, textureView);
            },
            depthClearDepth
        );
    }

}
