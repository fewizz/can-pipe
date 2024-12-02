package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.Pipelines;
import fewizz.canpipe.CanPipeVertexFormats;
import net.minecraft.client.renderer.chunk.SectionCompiler;

@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {

    @ModifyExpressionValue(
        method = "getOrBeginLayer",
        at = @At(
            value = "FIELD",
            target = "Lcom/mojang/blaze3d/vertex/DefaultVertexFormat;BLOCK:Lcom/mojang/blaze3d/vertex/VertexFormat;"
        )
    )
    VertexFormat replaceFormatOnBufferBuilderCreation(VertexFormat original) {
        return Pipelines.getCurrent() != null ? CanPipeVertexFormats.BLOCK : original;
    }

}
