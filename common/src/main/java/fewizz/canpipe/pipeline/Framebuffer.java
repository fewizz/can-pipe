package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import fewizz.canpipe.renderpearl.GpuDeviceExtended;

public class Framebuffer extends RenderTarget {

    public record ColorAttachment(Texture texture, int lod, int baseLayer, int layerCount, Vector4f clearColor) {}
    public record DepthAttachment(Texture texture, int lod, int baseLayer, int layerCount, double clearDepth) {}

    public final String name;

    public final List<ColorAttachment> colorAttachments;
    public final DepthAttachment depthAttachment;

    public final GpuTexture[] colorTextures;
    public final GpuTextureView[] colorTextureViews;

    private boolean destroyed = true;

    public Framebuffer(
        String name,
        List<ColorAttachment> colorAttachments,
        DepthAttachment depthAttachment
    ) {
        super(name, null, null);

        this.name = name;

        this.colorAttachments = Collections.unmodifiableList(colorAttachments);
        this.depthAttachment = depthAttachment;

        this.colorTextures = new GpuTexture[colorAttachments.size()];
        this.colorTextureViews = new GpuTextureView[colorAttachments.size()];

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

        this.depthTexture = null;

        this.colorTextureView = null;
        this.colorTexture = null;

        destroyed = true;
    }

    @Override
    public void createBuffers(int width, int height) {
        if (!destroyed) { throw new RuntimeException(); }

        for (int i = 0; i < this.colorTextures.length; ++i) {
            var colorAttachment = this.colorAttachments.get(i);

            this.colorTextures[i] = colorAttachment.texture.getTexture();
            this.colorTextureViews[i] = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                this.colorTextures[i], colorAttachment.lod, 1, colorAttachment.baseLayer, colorAttachment.layerCount
            );
        }

        if (this.colorTextures.length > 0) {
            this.colorTexture = this.colorTextures[0];
            this.colorTextureView = this.colorTextureViews[0];
        }

        if (this.depthAttachment != null) {
            this.depthTexture = this.depthAttachment.texture.getTexture();
            this.depthTextureView = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                this.depthTexture, this.depthAttachment.lod, 1, this.depthAttachment.baseLayer, this.depthAttachment.layerCount
            );
        }

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

    @Override
    public boolean hasDepth() {
        return this.depthTexture != null;
    }

    public Pair<List<GpuFormat>, @Nullable GpuFormat> getFormats() {
        List<GpuFormat> result = new ArrayList<>();
        for (var a : this.colorTextures) {
            result.add(a.getFormat());
        }
        return Pair.of(result, this.depthTexture != null ? this.depthTexture.getFormat() : null);
    }

}
