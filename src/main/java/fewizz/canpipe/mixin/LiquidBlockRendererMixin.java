package fewizz.canpipe.mixin;

import org.apache.commons.lang3.NotImplementedException;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.Pipelines;
import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {

    @Inject(
        method = "tesselate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/BlockAndTintGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            ordinal = 0
        )
    )
    void wrapVertexConsumerIfNeeded(CallbackInfo ci , @Local LocalRef<VertexConsumer> vc, @Local LocalRef<TextureAtlasSprite[]> sprites) {
        if (Pipelines.getCurrent() != null && vc.get() instanceof VertexConsumerExtended vce) {
            vc.set(new VertexConsumerWrapper(vce, sprites.get()));
        }
    }

    // TODO, VERY cursed :P
    static class VertexConsumerWrapper implements VertexConsumer {
        final VertexConsumerExtended original;
        final TextureAtlasSprite sprites[];
        final int spriteIndicies[] = new int[2];

        int vertexIndex = 0;
        final Vector3f[] veritices = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
        final Vector4i[] colors = new Vector4i[]{new Vector4i(), new Vector4i(), new Vector4i(), new Vector4i()};
        final Vector2f[] uvs = new Vector2f[]{new Vector2f(), new Vector2f(), new Vector2f(), new Vector2f()};
        final Vector2i[] uv2s = new Vector2i[]{new Vector2i(), new Vector2i(), new Vector2i(), new Vector2i()};
        final Vector3f[] normals = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};

        VertexConsumerWrapper(VertexConsumerExtended original, TextureAtlasSprite[] sprites) {
            this.original = original;
            this.sprites = sprites;

            var mc = Minecraft.getInstance();
            var atlas0 = mc.getModelManager().getAtlas(sprites[0].atlasLocation());
            var atlas1 = mc.getModelManager().getAtlas(sprites[1].atlasLocation());
            this.spriteIndicies[0] = ((TextureAtlasExtended) atlas0).indexBySprite(sprites[0]);
            this.spriteIndicies[1] = ((TextureAtlasExtended) atlas1).indexBySprite(sprites[1]);
        }

        void kick() {
            vertexIndex += 1;
            if (vertexIndex == 4) {
                int spriteIndex = this.spriteIndicies[0];  // still
                if (
                    uvs[0].x >= sprites[1].getU0() && uvs[0].y >= sprites[1].getV0() &&
                    uvs[1].x <= sprites[1].getU1() && uvs[1].y <= sprites[1].getV1()
                ) {
                    spriteIndex = this.spriteIndicies[1];  // flowing
                }

                Vector3f tangent = Pipelines.computeTangent(
                    veritices[0].x, veritices[0].y, veritices[0].z, uvs[0].x, uvs[0].y,
                    veritices[1].x, veritices[1].y, veritices[1].z, uvs[1].x, uvs[1].y,
                    veritices[2].x, veritices[2].y, veritices[2].z, uvs[2].x, uvs[2].y
                );
                for (int i = 0; i < 4; ++i ) {
                    original.addVertex(veritices[i]);
                    original.setColor(colors[i].x, colors[i].y, colors[i].z, colors[i].w);
                    original.setUv(uvs[i].x, uvs[i].y);
                    original.setUv2(uv2s[i].x, uv2s[i].y);
                    original.setNormal(normals[i].x, normals[i].y, normals[i].z);
                    original.setTangent(tangent.x, tangent.y, tangent.z);
                    original.setSpriteIndex(spriteIndex);
                }
                vertexIndex = 0;
            }
        }

        @Override
        public VertexConsumer addVertex(float x, float g, float h) {
            veritices[vertexIndex].set(x, g, h); return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            colors[vertexIndex].set(r, g, b, a); return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            uvs[vertexIndex].set(u, v); return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            throw new NotImplementedException();
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            uv2s[vertexIndex].set(u, v); return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            normals[vertexIndex].set(x, y, z);
            kick();
            return this;
        }

    }

}
