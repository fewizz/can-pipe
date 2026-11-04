package fewizz.canpipe.pipeline;

import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;

import fewizz.canpipe.renderpearl.GpuDeviceExtended;
import net.minecraft.client.renderer.texture.AbstractTexture;


public class Texture extends AbstractTexture {
    private final Supplier<GpuTexture> gpuTextureSupplier;
    private final boolean recreateOnResize;
    private final boolean closeSampler;

    public Texture(String name, GpuSampler sampler, boolean closeSampler, boolean recreateOnResize, Supplier<GpuTexture> gpuTextureUpdater) {
        this.gpuTextureSupplier = gpuTextureUpdater;
        this.recreateOnResize = recreateOnResize;
        this.sampler = sampler;
        this.closeSampler = closeSampler;
        this.texture = this.gpuTextureSupplier.get();
        this.textureView = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
            this.texture, 0, this.texture.getMipLevels(), 0, this.texture.getDepthOrLayers()
        );
    }

    @Override
    public void close() {
        super.close();
        if (closeSampler) { this.sampler.close(); }
    }

    void onWindowSizeChanged() {
        if (this.recreateOnResize) {
            this.texture.close();
            this.textureView.close();
            this.texture = this.gpuTextureSupplier.get();
            this.textureView = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                this.texture, 0, this.texture.getMipLevels(), 0, this.texture.getDepthOrLayers()
            );
        }
    }

}
