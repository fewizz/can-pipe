package fewizz.canpipe.mixin;

import java.util.function.Supplier;

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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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

    @Shadow
    private int vertices;

    @Shadow
    private int elementsToFill;

    @Shadow
    @Final
    private VertexFormat.Mode mode;

    @Shadow
    @Final
    private ByteBufferBuilder buffer;

    @Shadow
    @Final
    public VertexFormat format;

    @Shadow
    @Final
    private int vertexSize;

    @Shadow
    @Final
    private int[] offsetsByElement;

    @Shadow
    private long vertexPointer = -1L;

    @Shadow
    abstract protected long beginElement(VertexFormatElement vertexFormatElement);

    @Shadow
    private static byte normalIntValue(float f) {return 0;}

    @Unique
    private MaterialMap materialMap = null;

    @Unique
    private byte materialFlags = 0;

    @Unique
    private Supplier<TextureAtlasSprite> spriteSupplier = null;

    @Unique
    private boolean recomputeNormal = false;

    @Inject(
        method = "endLastVertex",
        at = @At(
            value = "FIELD",
            target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;elementsToFill:I"
        )
    )
    private void endLastVertex(CallbackInfo ci) {
        boolean lastVertex = (this.vertices % this.mode.primitiveLength) == 0;

        long l;
        if ((l = beginElement(VertexFormatElement.NORMAL)) != -1 && lastVertex) {
            canpipe_computeNormals(l);
        }
        if ((l = beginElement(CanPipe.VertexFormatElements.TANGENT)) != -1 && lastVertex) {
            int offsetToFirstVertex = -(this.mode.primitiveLength - 1);

            Vector3f tangent = computeTangent(
                canpipe_getPos(offsetToFirstVertex, 0),
                canpipe_getPos(offsetToFirstVertex, 1),
                canpipe_getPos(offsetToFirstVertex, 2),
                canpipe_getUV(offsetToFirstVertex, 0),
                canpipe_getUV(offsetToFirstVertex, 1),

                canpipe_getPos(offsetToFirstVertex+1, 0),
                canpipe_getPos(offsetToFirstVertex+1, 1),
                canpipe_getPos(offsetToFirstVertex+1, 2),
                canpipe_getUV(offsetToFirstVertex+1, 0),
                canpipe_getUV(offsetToFirstVertex+1, 1),

                canpipe_getPos(offsetToFirstVertex+2, 0),
                canpipe_getPos(offsetToFirstVertex+2, 1),
                canpipe_getPos(offsetToFirstVertex+2, 2),
                canpipe_getUV(offsetToFirstVertex+2, 0),
                canpipe_getUV(offsetToFirstVertex+2, 1)
            );
            for (int i = offsetToFirstVertex; i <= 0; ++i) {
                MemoryUtil.memPutByte(l+i*this.vertexSize+0, normalIntValue(tangent.x));
                MemoryUtil.memPutByte(l+i*this.vertexSize+1, normalIntValue(tangent.y));
                MemoryUtil.memPutByte(l+i*this.vertexSize+2, normalIntValue(tangent.z));
                MemoryUtil.memPutByte(l+i*this.vertexSize+3, normalIntValue(1.0F));
            }
        }
        if ((l = beginElement(CanPipe.VertexFormatElements.MATERIAL_FLAGS)) != -1) {
            MemoryUtil.memPutByte(l, this.materialFlags);
        }

        canpipe_setAO(1.0F);
    }

    public void canpipe_computeNormals(long l) {
        int offsetToFirstVertex = -(this.mode.primitiveLength - 1);

        float
            x0 = canpipe_getPos(offsetToFirstVertex, 0),
            y0 = canpipe_getPos(offsetToFirstVertex, 1),
            z0 = canpipe_getPos(offsetToFirstVertex, 2),
            x1 = canpipe_getPos(offsetToFirstVertex+1, 0),
            y1 = canpipe_getPos(offsetToFirstVertex+1, 1),
            z1 = canpipe_getPos(offsetToFirstVertex+1, 2),
            x2 = canpipe_getPos(offsetToFirstVertex+2, 0),
            y2 = canpipe_getPos(offsetToFirstVertex+2, 1),
            z2 = canpipe_getPos(offsetToFirstVertex+2, 2);

        Vector3f normal0 = computeNormal(x0, y0, z0, x1, y1, z1, x2, y2, z2);

        if (this.mode.primitiveLength == 4) {
            float
                x3 = canpipe_getPos(offsetToFirstVertex+3, 0),
                y3 = canpipe_getPos(offsetToFirstVertex+3, 1),
                z3 = canpipe_getPos(offsetToFirstVertex+3, 2);

            Vector3f normal1 = computeNormal(x2, y2, z2, x3, y3, z3, x0, y0, z0);

            Vector3f mid = new Vector3f(normal0).add(normal1).normalize();

            int i = offsetToFirstVertex;
            MemoryUtil.memPutByte(l+this.vertexSize*i+0, normalIntValue(mid.x));
            MemoryUtil.memPutByte(l+this.vertexSize*i+1, normalIntValue(mid.y));
            MemoryUtil.memPutByte(l+this.vertexSize*i+2, normalIntValue(mid.z));

            i += 1;
            MemoryUtil.memPutByte(l+this.vertexSize*i+0, normalIntValue(normal0.x));
            MemoryUtil.memPutByte(l+this.vertexSize*i+1, normalIntValue(normal0.y));
            MemoryUtil.memPutByte(l+this.vertexSize*i+2, normalIntValue(normal0.z));

            i += 1;
            MemoryUtil.memPutByte(l+this.vertexSize*i+0, normalIntValue(mid.x));
            MemoryUtil.memPutByte(l+this.vertexSize*i+1, normalIntValue(mid.y));
            MemoryUtil.memPutByte(l+this.vertexSize*i+2, normalIntValue(mid.z));

            i += 1;
            MemoryUtil.memPutByte(l+this.vertexSize*i+0, normalIntValue(normal1.x));
            MemoryUtil.memPutByte(l+this.vertexSize*i+1, normalIntValue(normal1.y));
            MemoryUtil.memPutByte(l+this.vertexSize*i+2, normalIntValue(normal1.z));
        } else {
            for (int i = offsetToFirstVertex; i <= 0; ++i) {
                MemoryUtil.memPutByte(l+this.vertexSize*i+0, normalIntValue(normal0.x));
                MemoryUtil.memPutByte(l+this.vertexSize*i+1, normalIntValue(normal0.y));
                MemoryUtil.memPutByte(l+this.vertexSize*i+2, normalIntValue(normal0.z));
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
        boolean lastVertex = !this.mode.connectedPrimitives && (this.vertices % this.mode.primitiveLength) == 0;

        TextureAtlasSprite sprite = lastVertex && this.spriteSupplier != null ? this.spriteSupplier.get() : null;

        long l;
        if ((l = this.beginElement(CanPipe.VertexFormatElements.SPRITE_INDEX)) != -1 && lastVertex) {
            int index = sprite != null ? ((TextureAtlasSpriteExtended) sprite).getIndex() : -1;
            for (int i = -(this.mode.primitiveLength - 1); i <= 0; ++i) {
                MemoryUtil.memPutInt(l + i*this.vertexSize, index);
            }
        }
        if ((l = this.beginElement(CanPipe.VertexFormatElements.MATERIAL_INDEX)) != -1 && lastVertex) {
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
                MemoryUtil.memPutInt(l+i*this.vertexSize, index);
            }
        }
    }

    @ModifyExpressionValue(
        method = "setNormal",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;beginElement(Lcom/mojang/blaze3d/vertex/VertexFormatElement;)J"
        )
    )
    private long onSetNormal(long l) {
        if (this.recomputeNormal && l != -1) {
            boolean lastVertex = (this.vertices % this.mode.primitiveLength) == 0;
            if (lastVertex) {
                canpipe_computeNormals(l);
            }
            return -1;
        }
        return l;
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
        if (glint) { this.materialFlags |= 1 << 0; }
        else {       this.materialFlags &= ~(1 << 0); }
    }

    @Override
    public void canpipe_recomputeNormal(boolean recompute) {
        this.recomputeNormal = recompute;
    }

    @Override
    public float canpipe_getUV(int vertexOffset, int element) {
        long uvOffset = this.offsetsByElement[VertexFormatElement.UV0.id()];
        long o = this.vertexPointer + (long) this.vertexSize*vertexOffset + uvOffset;
        return MemoryUtil.memGetFloat(o + (long) element*Float.BYTES);
    }

    public float canpipe_getPos(int vertexOffset, int element) {
        long posOffset = this.offsetsByElement[VertexFormatElement.POSITION.id()];
        long o = this.vertexPointer + (long) this.vertexSize*vertexOffset + posOffset;
        return MemoryUtil.memGetFloat(o + (long) element*Float.BYTES);
    }

    @Unique
    private static Vector3f computeTangent(
        float x0, float y0, float z0, float u0, float v0,
        float x1, float y1, float z1, float u1, float v1,
        float x2, float y2, float z2, float u2, float v2
    ) {
        // taken from frex
        final float dv0 = v1 - v0;
        final float dv1 = v2 - v1;
        final float du0 = u1 - u0;
        final float du1 = u2 - u1;
        final float inverseLength = 1.0f / (du0 * dv1 - du1 * dv0);

        final float tx = inverseLength * (dv1 * (x1 - x0) - dv0 * (x2 - x1));
        final float ty = inverseLength * (dv1 * (y1 - y0) - dv0 * (y2 - y1));
        final float tz = inverseLength * (dv1 * (z1 - z0) - dv0 * (z2 - z1));

        return new Vector3f(tx, ty, tz).normalize();
    }

    @Unique
    private static Vector3f computeNormal(
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2
    ) {
        final float dx0 = x2 - x1;
        final float dy0 = y2 - y1;
        final float dz0 = z2 - z1;
        final float dx1 = x0 - x1;
        final float dy1 = y0 - y1;
        final float dz1 = z0 - z1;

        float nx = dy0 * dz1 - dz0 * dy1;
        float ny = dz0 * dx1 - dx0 * dz1;
        float nz = dx0 * dy1 - dy0 * dx1;

        return new Vector3f(nx, ny, nz).normalize();
    }

}
