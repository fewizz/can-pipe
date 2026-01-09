package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

@Mixin(VertexFormat.Builder.class)
public class VertexFormatBuilderMixin {

    @Shadow
    private int offset;

    @Inject(
        method = "add",
        at = @At("HEAD"),
        remap = false
    )
    void onAdd(
        CallbackInfoReturnable<VertexFormat.Builder> cir,
        @Local String name, @Local VertexFormatElement element
    ) {
        // Fixes POSITION_COLOR_NORMAL_LINE_WIDTH vertex format
        if (name.equals("LineWidth") && (this.offset % 4) == 3) {
            ++this.offset;
        }
    }

}
