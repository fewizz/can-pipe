package fewizz.canpipe.b3d;

import java.util.ServiceLoader;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.systems.RenderSystem;

public abstract class GLService {

    private static ServiceLoader<GLService> loader = ServiceLoader.load(GLService.class);

    public abstract GlDevice realGLDevice();

    public static GlDevice getRealGLDevice() {
        for (GLService s : GLService.loader) {
            return s.realGLDevice();
        }
        return (GlDevice) RenderSystem.getDevice();
    }

}
