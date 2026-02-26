package fewizz.canpipe.b3d.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuDeviceBackend;

@Mixin(value = GpuDevice.class, priority = 1001)
public interface GpuDeviceAccessor {

    @Accessor("backend") GpuDeviceBackend canpipe_getBackend();

}
