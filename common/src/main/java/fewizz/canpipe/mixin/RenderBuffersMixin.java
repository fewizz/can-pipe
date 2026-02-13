package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import fewizz.canpipe.mixininterface.RenderBuffersExtended;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;

@Mixin(RenderBuffers.class)
public class RenderBuffersMixin implements RenderBuffersExtended {

    private MultiBufferSource.BufferSource canpipe_bufferSourceOverride = null;

    @Override
    public void canpipe_setBufferSourceOverride(BufferSource source) {
        this.canpipe_bufferSourceOverride = source;
    }

    @ModifyReturnValue(method = "bufferSource", at = @At("RETURN"))
    public MultiBufferSource.BufferSource bufferSource(MultiBufferSource.BufferSource original) {
        if (this.canpipe_bufferSourceOverride != null) {
            original = this.canpipe_bufferSourceOverride;
        }

        return original;
    }

}
