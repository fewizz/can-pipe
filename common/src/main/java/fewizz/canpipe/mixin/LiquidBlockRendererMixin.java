package fewizz.canpipe.mixin;

import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.FluidState;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {

    @Inject(
        method = "tesselate",
        at = @At("HEAD")
    )
    void wrapVertexConsumerIfNeeded(
        CallbackInfo ci,
        @Local(argsOnly = true) FluidState fs,
        @Local(argsOnly = true) VertexConsumer vc
    ) {
        if (
            vc instanceof VertexConsumerExtended vce &&
            vc instanceof BufferBuilder bb &&
            bb.format.contains(CanPipe.VertexFormatElements.SPRITE_INDEX)
        ) {

            @SuppressWarnings("deprecation")
            TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
            var sprites = ((TextureAtlasAccessor) atlas).canpipe_getSprites();

            vce.canpipe_setSpriteSupplier(() -> {
                MutableObject<TextureAtlasSprite> result = new MutableObject<>();

                float u0 = vce.canpipe_getUV(0, 0);
                float v0 = vce.canpipe_getUV(0, 1);

                float u1 = vce.canpipe_getUV(-1, 0);
                float v1 = vce.canpipe_getUV(-1, 1);

                float u2 = vce.canpipe_getUV(-2, 0);
                float v2 = vce.canpipe_getUV(-2, 1);

                for (var sprite : sprites.values()) {
                    if (
                        spriteContainsUV(sprite, u0, v0) &&
                        spriteContainsUV(sprite, u1, v1) &&
                        spriteContainsUV(sprite, u2, v2)
                    ) {
                        result.setValue(sprite);
                    }
                }

                return result.getValue();
            });

            MaterialMap materialMap = MaterialMaps.getForFluid(fs.getType());
            vce.canpipe_setSharedMaterialMap(materialMap);

            vce.canpipe_recomputeNormal(true);
        }
    }

    @Inject(
        method = "tesselate",
        at = @At("RETURN")
    )
    void resetVertexConsumer(
        CallbackInfo ci,
        @Local(argsOnly = true) VertexConsumer vc
    ) {
        if (vc instanceof VertexConsumerExtended vce) {
            vce.canpipe_setSpriteSupplier(null);
            vce.canpipe_setSharedMaterialMap(null);
            vce.canpipe_recomputeNormal(false);
        }
    }

    @Unique
    private static boolean spriteContainsUV(TextureAtlasSprite sprite, float u, float v) {
        return sprite.getU0() <= u && u <= sprite.getU1() && sprite.getV0() <= v && v <= sprite.getV1();
    }

}
