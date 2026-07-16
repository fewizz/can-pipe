package fewizz.canpipe.b3d.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vulkan.VulkanCommandEncoder;

import fewizz.canpipe.b3d.CommandEncoderBackendExtended;

@Mixin(VulkanCommandEncoder.class)
public abstract class VkCommandEncoderMixin implements CommandEncoderBackendExtended {

    @Unique private int canpipe_clearBaseLevel = -1;
    @Unique private int canpipe_clearLevelCount = -1;
    @Unique private int canpipe_clearBaseLayer = -1;
    @Unique private int canpipe_clearLayerCount = -1;

    @Override
    public void canpipe_clearDepthTexture(
        GpuTexture texture, double depth, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount
    ) {
        try {
            this.canpipe_clearBaseLevel = baseMipLevel;
            this.canpipe_clearLevelCount = levelCount;
            this.canpipe_clearBaseLayer = baseArrayLayer;
            this.canpipe_clearLayerCount = layerCount;
            this.clearDepthTexture(texture, depth);
        }
        finally {
            this.canpipe_clearBaseLevel = -1;
            this.canpipe_clearLevelCount = -1;
            this.canpipe_clearBaseLayer = -1;
            this.canpipe_clearLayerCount = -1;
        }
    }

    @Override
    public void canpipe_clearColorTexture(
        GpuTexture texture, Vector4f color, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount
    ) {
        try {
            this.canpipe_clearBaseLevel = baseMipLevel;
            this.canpipe_clearLevelCount = levelCount;
            this.canpipe_clearBaseLayer = baseArrayLayer;
            this.canpipe_clearLayerCount = layerCount;
            this.clearColorTexture(texture, color);
        }
        finally {
            this.canpipe_clearBaseLevel = -1;
            this.canpipe_clearLevelCount = -1;
            this.canpipe_clearBaseLayer = -1;
            this.canpipe_clearLayerCount = -1;
        }
    }

    public void canpipe_blitImage(
        GpuTexture srcTexture,
        GpuTexture dstTexture
    ) {
        // TODO
    }

    @ModifyArg(
        method = {"clearDepthTextureUnsynced", "clearColorTextureUnsynced"},
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkImageSubresourceRange;baseMipLevel(I)Lorg/lwjgl/vulkan/VkImageSubresourceRange;"
        )
    )
    int setClearBaseLevel(int baseLevel) {
        return this.canpipe_clearBaseLevel != -1 ? this.canpipe_clearBaseLevel : baseLevel;
    }

    @ModifyArg(
        method = {"clearDepthTextureUnsynced", "clearColorTextureUnsynced"},
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkImageSubresourceRange;levelCount(I)Lorg/lwjgl/vulkan/VkImageSubresourceRange;"
        )
    )
    int setClearLevelCount(int levelCount) {
        return this.canpipe_clearLevelCount != -1 ? this.canpipe_clearLevelCount : levelCount;
    }

    @ModifyArg(
        method = {"clearDepthTextureUnsynced", "clearColorTextureUnsynced"},
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkImageSubresourceRange;baseArrayLayer(I)Lorg/lwjgl/vulkan/VkImageSubresourceRange;"
        )
    )
    int setClearBaseLayer(int baseLayer) {
        return this.canpipe_clearBaseLayer != -1 ? this.canpipe_clearBaseLayer : baseLayer;
    }

    @ModifyArg(
        method = {"clearDepthTextureUnsynced", "clearColorTextureUnsynced"},
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkImageSubresourceRange;layerCount(I)Lorg/lwjgl/vulkan/VkImageSubresourceRange;"
        )
    )
    int setClearLayerCount(int layerCount) {
        return this.canpipe_clearLayerCount != -1 ? this.canpipe_clearLayerCount : layerCount;
    }

}
