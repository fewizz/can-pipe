package fewizz.canpipe.mixin;

import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.mixininterface.TextureAtlasSpriteExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements VertexConsumerExtended {

    @Shadow private int vertices;
    @Shadow private int elementsToFill;
    @Shadow @Final private VertexFormat.Mode mode;
    @Shadow @Final private ByteBufferBuilder buffer;
    @Shadow @Final public VertexFormat format;
    @Shadow @Final private int vertexSize;
    @Shadow @Final private int[] offsetsByElement;
    @Shadow private long vertexPointer = -1L;

    @Shadow abstract protected long beginElement(VertexFormatElement vertexFormatElement);
    @Shadow private static byte normalIntValue(float f) {return 0;}

    @Unique private MaterialMap materialMap = null;
    @Unique private byte materialFlags = 0;
    @Unique private Supplier<TextureAtlasSprite> spriteSupplier = null;
    @Unique private boolean recomputeNormal = false;

    @Inject(
        method = "endLastVertex",
        at = @At(
            value = "FIELD",  // after checking that this.vertices != 0
            target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;elementsToFill:I"
        )
    )
    private void endLastVertex(CallbackInfo ci) {
        long normalPtr = this.beginElement(VertexFormatElement.NORMAL);
        long tangentPtr = this.beginElement(CanPipe.VertexFormatElements.TANGENT);
        long materialFlagsPtr = this.beginElement(CanPipe.VertexFormatElements.MATERIAL_FLAGS);

        if (materialFlagsPtr != -1) {
            MemoryUtil.memPutByte(materialFlagsPtr, this.materialFlags);
        }

        this.canpipe_setAO(1.0F);

        boolean lastVertex = (this.vertices % this.mode.primitiveLength) == 0;
        if (!lastVertex || !(normalPtr != -1 || tangentPtr != -1)) {
            return;
        }

        long posPtr = this.vertexPointer + this.offsetsByElement[VertexFormatElement.POSITION.id()];

        int offsetToFirstVertex = -(this.mode.primitiveLength - 1);

        float
            x0 = this.canpipe_getPos(posPtr, offsetToFirstVertex+0, 0),
            y0 = this.canpipe_getPos(posPtr, offsetToFirstVertex+0, 1),
            z0 = this.canpipe_getPos(posPtr, offsetToFirstVertex+0, 2),
            x1 = this.canpipe_getPos(posPtr, offsetToFirstVertex+1, 0),
            y1 = this.canpipe_getPos(posPtr, offsetToFirstVertex+1, 1),
            z1 = this.canpipe_getPos(posPtr, offsetToFirstVertex+1, 2),
            x2 = this.canpipe_getPos(posPtr, offsetToFirstVertex+2, 0),
            y2 = this.canpipe_getPos(posPtr, offsetToFirstVertex+2, 1),
            z2 = this.canpipe_getPos(posPtr, offsetToFirstVertex+2, 2);

        Vector3f normal0 = computeNormal(x0, y0, z0, x1, y1, z1, x2, y2, z2);

        if (normalPtr != -1) {
            if (this.mode.primitiveLength == 4) {
                float
                    x3 = this.canpipe_getPos(posPtr, offsetToFirstVertex+3, 0),
                    y3 = this.canpipe_getPos(posPtr, offsetToFirstVertex+3, 1),
                    z3 = this.canpipe_getPos(posPtr, offsetToFirstVertex+3, 2);

                Vector3f normal1 = computeNormal(x2, y2, z2, x3, y3, z3, x0, y0, z0);

                Vector3f mid = new Vector3f(normal0).add(normal1).normalize();

                int i = offsetToFirstVertex;
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+0, normalIntValue(mid.x));
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+1, normalIntValue(mid.y));
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+2, normalIntValue(mid.z));

                i += 1;
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+0, normalIntValue(normal0.x));
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+1, normalIntValue(normal0.y));
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+2, normalIntValue(normal0.z));

                i += 1;
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+0, normalIntValue(mid.x));
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+1, normalIntValue(mid.y));
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+2, normalIntValue(mid.z));

                i += 1;
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+0, normalIntValue(normal1.x));
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+1, normalIntValue(normal1.y));
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+2, normalIntValue(normal1.z));
            } else {
                for (int i = offsetToFirstVertex; i <= 0; ++i) {
                    MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+0, normalIntValue(normal0.x));
                    MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+1, normalIntValue(normal0.y));
                    MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+2, normalIntValue(normal0.z));
                }
            }
        }

        if (tangentPtr != -1) {
            long uvPtr = this.vertexPointer + this.offsetsByElement[VertexFormatElement.UV0.id()];
            float
                u0 = this.canpipe_getUV(uvPtr, offsetToFirstVertex+0, 0),
                v0 = this.canpipe_getUV(uvPtr, offsetToFirstVertex+0, 1),
                u1 = this.canpipe_getUV(uvPtr, offsetToFirstVertex+1, 0),
                v1 = this.canpipe_getUV(uvPtr, offsetToFirstVertex+1, 1),
                u2 = this.canpipe_getUV(uvPtr, offsetToFirstVertex+2, 0),
                v2 = this.canpipe_getUV(uvPtr, offsetToFirstVertex+2, 1);
            Pair<Vector3f, Boolean> tangentPair = computeTangent(
                normal0,
                x0, y0, z0, u0, v0,
                x1, y1, z1, u1, v1,
                x2, y2, z2, u2, v2
            );
            Vector3f tangent = tangentPair.getLeft();
            boolean inverse = tangentPair.getRight();
            for (int i = offsetToFirstVertex; i <= 0; ++i) {
                MemoryUtil.memPutByte(tangentPtr+i*this.vertexSize+0, normalIntValue(tangent.x));
                MemoryUtil.memPutByte(tangentPtr+i*this.vertexSize+1, normalIntValue(tangent.y));
                MemoryUtil.memPutByte(tangentPtr+i*this.vertexSize+2, normalIntValue(tangent.z));
                MemoryUtil.memPutByte(tangentPtr+i*this.vertexSize+3, normalIntValue(inverse ? -1.0F : 1.0F));
            }
        }
    }

    @Inject(
        method = "setUv",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/system/MemoryUtil;memPutFloat(JF)V",
            ordinal = 1,  // after uv set
            shift = Shift.AFTER,
            remap = false
        )
    )
    void afterUVSet(float u, float v, CallbackInfoReturnable<VertexConsumer> cir) {
        long spriteIndexPtr = this.beginElement(CanPipe.VertexFormatElements.SPRITE_INDEX);
        long materialIndexPtr = this.beginElement(CanPipe.VertexFormatElements.MATERIAL_INDEX);

        boolean lastVertex = (this.vertices % this.mode.primitiveLength) == 0;

        if (!lastVertex || !(spriteIndexPtr != -1 || materialIndexPtr != -1)) {
            return;
        }

        TextureAtlasSprite sprite = lastVertex && this.spriteSupplier != null ? this.spriteSupplier.get() : null;

        if (spriteIndexPtr != -1) {
            int index = sprite != null ? ((TextureAtlasSpriteExtended) sprite).getIndex() : -1;
            for (int i = -(this.mode.primitiveLength - 1); i <= 0; ++i) {
                MemoryUtil.memPutInt(spriteIndexPtr + i*this.vertexSize, index);
            }
        }

        if (materialIndexPtr != -1) {
            Material material = null;
            if (this.materialMap != null) {
                if (this.materialMap.spriteMap != null && sprite != null) {
                    Minecraft mc = Minecraft.getInstance();
                    TextureAtlas atlas = mc.getModelManager().getAtlas(sprite.atlasLocation());

                    for (var kv : this.materialMap.spriteMap.entrySet()) {
                        if (atlas.getSprite(kv.getKey()) == sprite) {
                            material = kv.getValue();
                        }
                    }
                }
                if (material == null) {
                    material = materialMap.defaultMaterial;
                }
            }

            int index = material != null ? Materials.id(material) : -1;
            for (int i = -(this.mode.primitiveLength - 1); i <= 0; ++i) {
                MemoryUtil.memPutInt(materialIndexPtr+i*this.vertexSize, index);
            }
        }
    }

    @Inject(
        method = "setNormal",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSetNormal(CallbackInfoReturnable<VertexConsumer> cir) {
        if (this.recomputeNormal) {
            cir.setReturnValue(this);
        }
    }

    @Override
    public void canpipe_setAO(float ao) {
        long l = this.beginElement(CanPipe.VertexFormatElements.AO);
        if (l != -1) {
            MemoryUtil.memPutFloat(l, ao);
        }
    }

    @Override
    public void canpipe_setSpriteSupplier(Supplier<TextureAtlasSprite> spriteSupplier) {
        this.spriteSupplier = spriteSupplier;
    }

    @Override
    public void canpipe_setSharedMaterialMap(MaterialMap materialmap) {
        this.materialMap = materialmap;
    }

    @Override
    public void canpipe_setSharedGlint(boolean glint) {
        if (glint) { this.materialFlags |=   1 << 0;  }
        else       { this.materialFlags &= ~(1 << 0); }
    }

    @Override
    public void canpipe_recomputeNormal(boolean recompute) {
        this.recomputeNormal = recompute;
    }

    @Unique
    private final float canpipe_getUV(long uvPtr, int vertexOffset, int element) {
        return MemoryUtil.memGetFloat(uvPtr + (vertexOffset*this.vertexSize + element*Float.BYTES));
    }

    @Override
    public float canpipe_getUV(int vertexOffset, int element) {
        long uvPtr = this.vertexPointer + this.offsetsByElement[VertexFormatElement.UV0.id()];
        return this.canpipe_getUV(uvPtr, vertexOffset, element);
    }

    @Unique
    private final float canpipe_getPos(long posPtr, int vertexOffset, int element) {
        return MemoryUtil.memGetFloat(posPtr + (vertexOffset*this.vertexSize + element*Float.BYTES));
    }

    /**
     * Taken from
     * <a href="https://github.com/vram-guild/frex/blob/1.19/common/src/main/java/io/vram/frex/base/renderer/mesh/BaseQuadView.java#L261">
     * BaseQuadView.computePackedFaceTangent
     * </a>
     * method, but i have so many questions...
     * <p>
     * Why {@code inverseLength} is named like that?
     * Resulting {@code vec3(tx, ty, tz)} is almost never has length 1.0, and
     * {@code PackedVector3f.pack(tx, ty, tz, inverted)} packs unnormalized vector, clamping components
     * <p>
     * Why bitangent isn't provided to shaders? tangent and bitanget are not necessarily orthogonal
    */
    @Unique
    private static Pair<Vector3f, Boolean> computeTangent(
        Vector3f normal,
        float x0, float y0, float z0, float u0, float v0,
        float x1, float y1, float z1, float u1, float v1,
        float x2, float y2, float z2, float u2, float v2
    ) {
        float dv0 = v0 - v1;
        float du0 = u0 - u1;
        float dv1 = v2 - v1;
        float du1 = u2 - u1;

        float dx0 = x0 - x1;
        float dy0 = y0 - y1;
        float dz0 = z0 - z1;
        float dx1 = x2 - x1;
        float dy1 = y2 - y1;
        float dz1 = z2 - z1;

        // we don't care about magnitudes, assume that TBN has orthonormal basis
        float determinantSign = Math.signum(du0*dv1 - du1*dv0);

        float tx = determinantSign * ( dv1*dx0 + -dv0*dx1);
        float ty = determinantSign * ( dv1*dy0 + -dv0*dy1);
        float tz = determinantSign * ( dv1*dz0 + -dv0*dz1);

        float bx = determinantSign * (-du1*dx0 +  du0*dx1);
        float by = determinantSign * (-du1*dy0 +  du0*dy1);
        float bz = determinantSign * (-du1*dz0 +  du0*dz1);

        // cross product of tangent and bitangent
        float cx = ty*bz - tz*by;
        float cy = tz*bx - tx*bz;
        float cz = tx*by - ty*bx;

        return Pair.of(
            new Vector3f(tx, ty, tz).normalize(),
            // if true, then bitangent should be inversed
            normal.x*cx + normal.y*cy + normal.z*cz < 0.0
        );
    }

    @Unique
    private static Vector3f computeNormal(
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2
    ) {
        float dx0 = x0 - x1;
        float dy0 = y0 - y1;
        float dz0 = z0 - z1;
        float dx1 = x2 - x1;
        float dy1 = y2 - y1;
        float dz1 = z2 - z1;

        float nx = -(dy0*dz1 - dz0*dy1);
        float ny = -(dz0*dx1 - dx0*dz1);
        float nz = -(dx0*dy1 - dy0*dx1);

        return new Vector3f(nx, ny, nz).normalize();
    }

}
