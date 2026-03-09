package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
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
            vce.canpipe_getVertexFormat().contains(CanPipe.VertexFormatElements.SPRITE_INDEX)
        ) {

            TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
            var sprites = ((TextureAtlasExtended) atlas).canpipe_getSprites();

            // TODO. Disgusting. Can't think of other universal way for finding sprite
            vce.canpipe_setScopedSpriteSupplier(() -> {
                float u0 = vce.canpipe_getU(0);
                float v0 = vce.canpipe_getV(0);

                float u1 = vce.canpipe_getU(-1);
                float v1 = vce.canpipe_getV(-1);

                float u2 = vce.canpipe_getU(-2);
                float v2 = vce.canpipe_getV(-2);

                for (var sprite : sprites.values()) {
                    if (
                        spriteContainsUV(sprite, u0, v0) &&
                        spriteContainsUV(sprite, u1, v1) &&
                        spriteContainsUV(sprite, u2, v2)
                    ) {
                        return sprite;
                    }
                }

                return null;
            });

            MaterialMap materialMap = MaterialMaps.getForFluid(fs.getType());
            vce.canpipe_setScopedMaterialMap(materialMap);

            vce.canpipe_forceNormalRecomputation(true);
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
            vce.canpipe_setScopedSpriteSupplier(null);
            vce.canpipe_setScopedMaterialMap(null);
            vce.canpipe_forceNormalRecomputation(false);
        }
    }

    @Unique
    private static boolean spriteContainsUV(TextureAtlasSprite sprite, float u, float v) {
        return
            sprite.getU0() <= u && u <= sprite.getU1() &&
            sprite.getV0() <= v && v <= sprite.getV1();
    }

}
