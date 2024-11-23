package fewizz.canpipe.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import fewizz.canpipe.CanPipeRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public class RenderSectionMixin {

    @ModifyExpressionValue(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderType;chunkBufferLayers()Ljava/util/List;"
        )
    )
    List<RenderType> replaceChunkBufferLayers(List<RenderType> original) {
        return CanPipeRenderTypes.chunkBufferLayers();
    }

}
