package fewizz.canpipe.b3d;

import java.util.ServiceLoader;

import com.mojang.blaze3d.systems.GpuDeviceBackend;

public abstract class RealGpuDeviceProviderService {

    private static ServiceLoader<RealGpuDeviceProviderService> loader = ServiceLoader.load(RealGpuDeviceProviderService.class);

    public abstract GpuDeviceBackend realGpuDevice();

    public static GpuDeviceBackend getRealGpuDevice() {
        for (RealGpuDeviceProviderService s : RealGpuDeviceProviderService.loader) {
            return s.realGpuDevice();
        }
        return null; //RenderSystem.getDevice();
    }

}
