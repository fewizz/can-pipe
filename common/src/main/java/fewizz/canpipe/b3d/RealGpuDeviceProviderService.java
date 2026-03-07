package fewizz.canpipe.b3d;

import java.util.ServiceLoader;

import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.RenderSystem;

import fewizz.canpipe.b3d.mixin.GpuDeviceAccessor;

public abstract class RealGpuDeviceProviderService {

    private static ServiceLoader<RealGpuDeviceProviderService> loader = ServiceLoader.load(RealGpuDeviceProviderService.class);

    public abstract GpuDeviceBackend impl_getRealGpuDeviceBackend();

    public static GpuDeviceBackend getRealGpuDeviceBackend() {
        for (RealGpuDeviceProviderService s : RealGpuDeviceProviderService.loader) {
            return s.impl_getRealGpuDeviceBackend();
        }
        return ((GpuDeviceAccessor) RenderSystem.getDevice()).canpipe_getBackend();
    }

}
