package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.CanPipeVertexFormats;
import fewizz.canpipe.Pipelines;
import net.minecraft.client.renderer.RenderType;

@Mixin(RenderType.class)
public class RenderTypeMixin {

    @ModifyReturnValue(method = "format", at = @At("RETURN"))
    public VertexFormat replaceFormat(VertexFormat format) {
        if (Pipelines.getCurrent() != null) {
            if (format == DefaultVertexFormat.BLOCK) {
                return CanPipeVertexFormats.BLOCK;
            }
            if (format == DefaultVertexFormat.NEW_ENTITY) {
                return CanPipeVertexFormats.NEW_ENTITY;
            }
            if (format == DefaultVertexFormat.PARTICLE) {
                return CanPipeVertexFormats.PARTICLE;
            }
        }
        return format;
    }

}
