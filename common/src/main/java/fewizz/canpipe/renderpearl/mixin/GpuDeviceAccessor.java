package fewizz.canpipe.renderpearl.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;

@Mixin(value = GpuDevice.class, priority = 1001)
public interface GpuDeviceAccessor {

    @Accessor("backend") GpuDeviceBackend canpipe_getBackend();

}
