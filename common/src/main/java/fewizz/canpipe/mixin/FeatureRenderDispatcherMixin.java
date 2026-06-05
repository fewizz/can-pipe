package fewizz.canpipe.mixin;

import org.objectweb.asm.Opcodes;
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
    @Unique private MultiBufferSource.BufferSource canpipe_crumblingBufferSourceOverride = null;
    // TODO: Should I something with outlineBufferSource?

    @Override public void canpipe_setBufferSourceOverride(BufferSource source) { this.canpipe_bufferSourceOverride = source; }
    @Override public void canpipe_setCrumblingBufferSourceOverride(BufferSource source) { this.canpipe_crumblingBufferSourceOverride = source; }

    @ModifyExpressionValue(
        method = {"renderSolidFeatures", "renderTranslucentFeatures"},
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;bufferSource:Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;",
            opcode = Opcodes.GETFIELD
        )
    )
    MultiBufferSource.BufferSource replaceBufferSource(MultiBufferSource.BufferSource original) {
        if (this.canpipe_bufferSourceOverride != null) {
            original = this.canpipe_bufferSourceOverride;
        }

        return original;
    }

    @ModifyExpressionValue(
        method = {"renderSolidFeatures", "renderTranslucentFeatures"},
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;crumblingBufferSource:Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;",
            opcode = Opcodes.GETFIELD
        )
    )
    MultiBufferSource.BufferSource replaceCrumblingBufferSource(MultiBufferSource.BufferSource original) {
        if (this.canpipe_crumblingBufferSourceOverride != null) {
            original = this.canpipe_crumblingBufferSourceOverride;
        }

        return original;
    }

}
