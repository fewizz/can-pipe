package fewizz.canpipe.compat.cinnabar.mixin;

import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import graphics.cinnabar.core.mercury.MercuryDeviceStartup;

@Mixin(MercuryDeviceStartup.class)
public class MercuryDeviceStartupMixin {

    @Shadow private static void logMissingFeature(String featureName) {}

    @Inject(
        method = "enableRequiredFeatures",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkPhysicalDeviceFeatures;samplerAnisotropy(Z)Lorg/lwjgl/vulkan/VkPhysicalDeviceFeatures;",
            shift = Shift.AFTER
        )
    )
    private static void enableIndependentBlendFeature(
        CallbackInfo ci,
        @Local VkPhysicalDeviceFeatures physicalDeviceFeatures10
    ) {
        physicalDeviceFeatures10.independentBlend(true);
    }

    @ModifyReturnValue(
        method = "hasAllRequiredFeatures("+
            "Lorg/lwjgl/vulkan/VkPhysicalDeviceFeatures;"+
            "Lorg/lwjgl/vulkan/VkPhysicalDeviceVulkan11Features;"+
            "Lorg/lwjgl/vulkan/VkPhysicalDeviceVulkan12Features;"+
            "Lorg/lwjgl/vulkan/VkPhysicalDeviceSynchronization2FeaturesKHR;"+
        ")Z",
        at = @At("RETURN")
    )
    private static boolean hasAllRequiredFeatures(
        boolean hasAllFeatures,
        @Local VkPhysicalDeviceFeatures physicalDeviceFeatures10
    ) {
        if (!physicalDeviceFeatures10.independentBlend()) {
            logMissingFeature("independentBlend");
            hasAllFeatures = false;
        }
        return hasAllFeatures;
    }

}
