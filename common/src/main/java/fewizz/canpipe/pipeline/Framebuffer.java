package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import net.minecraft.resources.Identifier;

public class Framebuffer extends RenderTarget {

    public final String name;

    private final IntFunction<Pair<GpuTexture, GpuTextureView>> colorAttachmentsSupplier;
    private final Supplier<Pair<GpuTexture, GpuTextureView>> depthAttachmentSupplier;

    public final GpuTexture[] colorTextures;
    public final GpuTextureView[] colorTextureViews;
    public final Vector4f[] colorTextureClearColors;

    public @Nullable final Double depthTextureClearDepth;

    private boolean destroyed = true;

    public Framebuffer(
        Identifier pipelineLocation,
        String name,
        IntFunction<Pair<GpuTexture, GpuTextureView>> colorTextureSupplier,
        Vector4f[] colorClearColors,
        Supplier<Pair<GpuTexture, GpuTextureView>> depthTextureSupplier,
        @Nullable Double depthClearDepth
    ) {
        super(name, null, null);

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

    public Pair<List<GpuFormat>, @Nullable GpuFormat> getFormats() {
        List<GpuFormat> result = new ArrayList<>();
        for (var a : this.colorTextures) {
            result.add(a.getFormat());
        }
        return Pair.of(result, this.depthTexture != null ? this.depthTexture.getFormat() : null);
    }

}
