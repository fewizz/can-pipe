package fewizz.canpipe.b3d.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.vulkan.VulkanDevice;

@Mixin(value = VulkanDevice.class, priority = 1001)
public interface VkDeviceAccessor {

    @Accessor("canpipe_pendingTextureViewBaseLayer") int get_canpipe_pendingTextureViewBaseLayer();
    @Accessor("canpipe_pendingTextureViewLayerCount") int get_canpipe_pendingTextureViewLayerCount();
    @Accessor("canpipe_addressModeW") AddressMode get_canpipe_addressModeW();
    @Accessor("canpipe_linearMipmap") Boolean get_canpipe_linearMipmap();
    @Accessor("canpipe_compareOp") CompareOp get_canpipe_compareOp();
    @Accessor("canpipe_compilationLog") void set_canpipe_compilationLog(String log);
    @Accessor("canpipe_expectedInputAttributes") Set<String> get_canpipe_expectedInputAttributes();

}
