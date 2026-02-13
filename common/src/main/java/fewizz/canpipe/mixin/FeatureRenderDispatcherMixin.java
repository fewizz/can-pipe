package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import fewizz.canpipe.mixininterface.FeatureRenderDispatcherExtended;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;

@Mixin(FeatureRenderDispatcher.class)
public class FeatureRenderDispatcherMixin implements FeatureRenderDispatcherExtended {

    @Unique private MultiBufferSource.BufferSource canpipe_bufferSourceOverride = null;

    @Override
    public void canpipe_setBufferSourceOverride(BufferSource source) {
        this.canpipe_bufferSourceOverride = source;
    }

    @ModifyExpressionValue(
        method = "renderAllFeatures",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;bufferSource:Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"
        )
    )
    MultiBufferSource.BufferSource renderAllFeatures(MultiBufferSource.BufferSource original) {
        if (this.canpipe_bufferSourceOverride != null) {
            original = this.canpipe_bufferSourceOverride;
        }

        return original;
    }

}
