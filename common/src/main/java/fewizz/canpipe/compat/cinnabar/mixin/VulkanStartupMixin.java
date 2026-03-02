package fewizz.canpipe.compat.cinnabar.mixin;

import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import graphics.cinnabar.loader.earlywindow.VulkanStartup;

@Mixin(VulkanStartup.class)
public class VulkanStartupMixin {

    @Shadow private static void logMissingFeature(String featureName) {}

    @ModifyReturnValue(
        method = "hasAllRequiredFeatures",
        at = @At("RETURN")
    )
    private static boolean hasAllRequiredFeatures(boolean hasAllFeatures, VkPhysicalDeviceFeatures physicalDeviceFeatures10) {
        if (!physicalDeviceFeatures10.independentBlend()) {
            logMissingFeature("independentBlend");
            hasAllFeatures = false;
        }
        return hasAllFeatures;
    }

    @Inject(
        method = "createLogicalDeviceAndQueues",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkPhysicalDeviceFeatures;multiDrawIndirect(Z)Lorg/lwjgl/vulkan/VkPhysicalDeviceFeatures;"
        )
    )
    private static void enableIndependentBlendFeature(CallbackInfoReturnable<Void> cir, @Local VkPhysicalDeviceFeatures physicalDeviceFeatures10) {
        physicalDeviceFeatures10.independentBlend(true);
    }

}
