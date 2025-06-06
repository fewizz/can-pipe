package fewizz.canpipe.mixin.m03_core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
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
    VertexFormat replaceFormatOnBufferBuilderCreation(
        VertexFormat original,
        @Local(argsOnly = true) ChunkSectionLayer chunkSectionLayer
    ) {
        return chunkSectionLayer.pipeline().getVertexFormat();
    }

}
