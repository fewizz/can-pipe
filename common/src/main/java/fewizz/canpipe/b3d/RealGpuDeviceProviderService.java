package fewizz.canpipe.b3d;

import java.util.ServiceLoader;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;

public abstract class RealGpuDeviceProviderService {

    private static ServiceLoader<RealGpuDeviceProviderService> loader = ServiceLoader.load(RealGpuDeviceProviderService.class);

    public abstract GpuDevice realGpuDevice();

    public static GpuDevice getRealGpuDevice() {
        for (RealGpuDeviceProviderService s : RealGpuDeviceProviderService.loader) {
            return s.realGpuDevice();
        }
        return RenderSystem.getDevice();
    }

}
