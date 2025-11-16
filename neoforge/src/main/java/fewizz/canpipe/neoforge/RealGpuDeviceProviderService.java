package fewizz.canpipe.neoforge;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;

import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuDevice;

public class RealGpuDeviceProviderService extends fewizz.canpipe.b3d.RealGpuDeviceProviderService {

    @Override
    public GpuDevice realGpuDevice() {
        var device = RenderSystem.getDevice();
        if (device instanceof ValidationGpuDevice) {
            device = ((ValidationGpuDevice) device).getRealDevice();
        }
        return device;
    }
    
}
