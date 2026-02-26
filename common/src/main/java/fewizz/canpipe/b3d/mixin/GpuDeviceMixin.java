package fewizz.canpipe.b3d.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.GpuDeviceBackendExtended;
import fewizz.canpipe.b3d.GpuDeviceExtended;

@Mixin(GpuDevice.class)
public class GpuDeviceMixin implements GpuDeviceExtended {

    @Final private GpuDeviceBackend backend;

    @Override
    public GpuTextureView canpipe_createTextureView(
        GpuTexture gpuTexture, int baseMip, int levelCount,
        int baseLayer, int layerCount // added
    ) {
        return ((GpuDeviceBackendExtended) this.backend).canpipe_createTextureView(
            gpuTexture, baseMip, levelCount, baseLayer, layerCount
        );
    }

}
