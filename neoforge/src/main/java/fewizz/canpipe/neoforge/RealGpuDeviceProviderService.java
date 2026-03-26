package fewizz.canpipe.neoforge;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.RenderSystem;

import fewizz.canpipe.b3d.mixin.GpuDeviceAccessor;

public class RealGpuDeviceProviderService extends fewizz.canpipe.b3d.RealGpuDeviceProviderService {

    @Override
    public GpuDeviceBackend impl_getRealGpuDeviceBackend() {
        return ((GpuDeviceAccessor) RenderSystem.getDevice()).canpipe_getBackend();
    }
    
}
