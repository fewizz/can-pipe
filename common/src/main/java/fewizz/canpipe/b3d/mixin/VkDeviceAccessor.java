package fewizz.canpipe.b3d.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.vulkan.VulkanDevice;

@Mixin(value = VulkanDevice.class, priority = 1001)
public interface VkDeviceAccessor {

    @Accessor("canpipe_pendingTextureViewBaseLayer") int get_canpipe_pendingTextureViewBaseLayer();

    @Accessor("canpipe_pendingTextureViewLayerCount") int get_canpipe_pendingTextureViewLayerCount();

}
