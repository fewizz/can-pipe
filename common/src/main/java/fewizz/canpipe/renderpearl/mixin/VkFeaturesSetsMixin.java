package fewizz.canpipe.renderpearl.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.google.common.collect.ImmutableSet;
import com.mojang.renderpearl.backend.vulkan.VulkanFeatureSets;
import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import com.mojang.renderpearl.backend.vulkan.init.VulkanFeature;
import com.mojang.renderpearl.backend.vulkan.init.VulkanPNextStruct;

@Mixin(VulkanFeatureSets.class)
public class VkFeaturesSetsMixin {

    @Shadow @Final public static VulkanPNextStruct VK10_FEATURES_STRUCT;
    @Shadow @Mutable public static FeatureSet REQUIRED_FEATURESET;

    @ModifyArg(
        method = "<clinit>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/renderpearl/backend/vulkan/init/FeatureSet;<init>(Ljava/lang/String;Ljava/util/Set;Ljava/util/Set;)V",
            ordinal = 0
        ),
        index = 2
    )
    private static Set<VulkanFeature> onClassInit(Set<VulkanFeature> features) {
        return ImmutableSet.<VulkanFeature>builder()
            .addAll(features)
            .add(new VulkanFeature(VK10_FEATURES_STRUCT, "independentBlend"))
            .build();
    }

}
