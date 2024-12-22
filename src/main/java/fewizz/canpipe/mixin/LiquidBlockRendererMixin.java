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

import fewizz.canpipe.material.Material;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.mixininterface.TextureAtlasSpriteExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FluidState;

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
    void wrapVertexConsumerIfNeeded(
        CallbackInfo ci,
        @Local FluidState fs,
        @Local LocalRef<VertexConsumer> vc,
        @Local LocalRef<TextureAtlasSprite[]> sprites
    ) {
        if (Pipelines.getCurrent() != null && vc.get() instanceof VertexConsumerExtended vce) {
            vc.set(new VertexConsumerWrapper(vce, sprites.get(), fs));
        }
    }

    // TODO, VERY cursed :P
    static class VertexConsumerWrapper implements VertexConsumer {
        final VertexConsumerExtended original;
        final TextureAtlasSprite sprites[];
        final FluidState fluidState;

        int vertexIndex = 0;
        final Vector3f[] veritices = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
        final Vector4i[] colors = new Vector4i[]{new Vector4i(), new Vector4i(), new Vector4i(), new Vector4i()};
        final Vector2f[] uvs = new Vector2f[]{new Vector2f(), new Vector2f(), new Vector2f(), new Vector2f()};
        final Vector2i[] uv2s = new Vector2i[]{new Vector2i(), new Vector2i(), new Vector2i(), new Vector2i()};
        final Vector3f[] normals = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};

        VertexConsumerWrapper(VertexConsumerExtended original, TextureAtlasSprite[] sprites, FluidState fs) {
            this.original = original;
            this.sprites = sprites;
            this.fluidState = fs;
        }

        void kick() {
            vertexIndex += 1;

            if (vertexIndex == 4) {
                boolean still = true;

                Vector3f normal = Pipelines.computeNormal(
                    veritices[0].x, veritices[0].y, veritices[0].z,
                    veritices[1].x, veritices[1].y, veritices[1].z,
                    veritices[2].x, veritices[2].y, veritices[2].z
                );

                if (
                    uvs[0].x >= sprites[1].getU0() && uvs[0].y >= sprites[1].getV0() &&
                    uvs[1].x <= sprites[1].getU1() && uvs[1].y <= sprites[1].getV1()
                ) {
                    still = false;
                }
                ResourceLocation rl = BuiltInRegistries.FLUID.getKey(fluidState.getType());
                MaterialMap materialMap = MaterialMaps.FLUIDS.get(rl);

                Material material = materialMap != null ? materialMap.defaultMaterial : null;
                original.setSharedMaterialIndex(material != null ? Materials.id(material) : -1);

                Vector3f tangent = Pipelines.computeTangent(
                    veritices[0].x, veritices[0].y, veritices[0].z, uvs[0].x, uvs[0].y,
                    veritices[1].x, veritices[1].y, veritices[1].z, uvs[1].x, uvs[1].y,
                    veritices[2].x, veritices[2].y, veritices[2].z, uvs[2].x, uvs[2].y
                );
                original.setSharedTangent(tangent.x, tangent.y, tangent.z);
                original.setSharedSpriteIndex(((TextureAtlasSpriteExtended) this.sprites[still ? 0 : 1]).getIndex());

                for (int i = 0; i < 4; ++i ) {
                    original.addVertex(veritices[i]);
                    original.setColor(colors[i].x, colors[i].y, colors[i].z, colors[i].w);
                    original.setUv(uvs[i].x, uvs[i].y);
                    original.setUv2(uv2s[i].x, uv2s[i].y);
                    original.setNormal(normal.x, normal.y, normal.z);  // TODO
                    original.inheritTangent();
                    original.inheritMaterialIndex();
                    original.inheritSpriteIndex();
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
