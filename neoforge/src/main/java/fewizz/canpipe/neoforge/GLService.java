package fewizz.canpipe.neoforge;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.systems.RenderSystem;

import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuDevice;

public class GLService extends fewizz.canpipe.b3d.GLService {

    @Override
    public GlDevice realGLDevice() {
        var device = RenderSystem.getDevice();
        if (device instanceof ValidationGpuDevice) {
            device = ((ValidationGpuDevice)device).getRealDevice();
        }
        return (GlDevice) device;
    }
    
}
