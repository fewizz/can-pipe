package fewizz.canpipe.b3d.mixin;

import java.util.Set;

import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.vulkan.VulkanBackend;
import com.mojang.blaze3d.vulkan.init.VulkanFeature;
import com.mojang.blaze3d.vulkan.init.VulkanPNextStruct;

@Mixin(VulkanBackend.class)
public class VkBackendMixin {

    @Shadow @Final public static VulkanPNextStruct VK10_FEATURES_STRUCT;
    @Shadow @Mutable public static Set<VulkanFeature> REQUIRED_DEVICE_FEATURES;

    @Inject(method = "<clinit>", at = @At(value = "RETURN"))
    private static void onClassInit(CallbackInfo ci) {
        REQUIRED_DEVICE_FEATURES = ImmutableSet.<VulkanFeature>builder()
            .addAll(REQUIRED_DEVICE_FEATURES)
            .add(new VulkanFeature(VK10_FEATURES_STRUCT, "independentBlend", VkPhysicalDeviceFeatures.INDEPENDENTBLEND))
            .build();
    }

}
