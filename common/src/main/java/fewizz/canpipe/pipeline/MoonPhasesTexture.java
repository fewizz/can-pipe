package fewizz.canpipe.pipeline;

import java.util.List;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

public class MoonPhasesTexture extends AbstractTexture {
    // In 1.21.11, `textures/environment/moon_phases.png` is divided into
    // moon/full_moon, moon/waning_gibbous, moon/third_quarter, moon/waning_crescent
    // moon/new_moon, moon/waxing_crescent, moon/first_quarter, moon/waxing_gibbous

    public MoonPhasesTexture() {
        Minecraft mc = Minecraft.getInstance();

        List<NativeImage> phases = List.of(
            "full_moon", "waning_gibbous", "third_quarter", "waning_crescent",
            "new_moon", "waxing_crescent", "first_quarter", "waxing_gibbous"
        ).stream().map(phaseName -> {
            try {
                var resource = mc.getResourceManager().getResourceOrThrow(Identifier.withDefaultNamespace("textures/environment/celestial/moon/"+phaseName+".png"));
                return NativeImage.read(resource.open());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();

        var gpuDevice = RenderSystem.getDevice();
        var commandEncoder = gpuDevice.createCommandEncoder();

        int w = phases.get(0).getWidth();
        int h = phases.get(0).getHeight();

        NativeImage nativeImage = new NativeImage(w*4, h*2, false);
        for (int y = 0; y < 2; ++y) {
            for (int x = 0; x < 4; ++x) {
                phases.get(y*4+x).copyRect(
                    nativeImage,
                    0, 0,  // src x/y
                    x*w, y*h,  // dst x/y
                    w, h,
                    false, false  // don't mirror
                );
            }
        }

        this.texture = gpuDevice.createTexture(
            "can-pipe: moon phases",
            GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
            GpuFormat.RGBA8_UNORM,
            w * 4, h * 2,
            1, 1
        );
        commandEncoder.writeToTexture(this.texture, nativeImage);

        this.textureView = gpuDevice.createTextureView(texture);
        this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
    }

}
