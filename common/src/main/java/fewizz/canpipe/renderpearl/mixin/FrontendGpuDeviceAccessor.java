package fewizz.canpipe.renderpearl.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.frontend.FrontendGpuDevice;

@Mixin(value = FrontendGpuDevice.class, priority = 1001)
public interface FrontendGpuDeviceAccessor {

    @Accessor("backend") GpuDeviceBackend canpipe_getBackend();

}
