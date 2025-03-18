package fewizz.canpipe.mixin;

import java.util.function.Consumer;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.NormalAndTangent;
import fewizz.canpipe.TangentSetter;
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

    @Shadow private long beginElement(VertexFormatElement vertexFormatElement) { return -1; }
    @Shadow private static byte normalIntValue(float f) { return 0; }

    @Unique private MaterialMap materialMap = null;
    @Unique private byte materialFlags = 0;
    @Unique private Supplier<TextureAtlasSprite> spriteSupplier = null;
    @Unique private boolean recomputeNormal = false;

    @ModifyExpressionValue(
        method = "endLastVertex",
        at = @At(
            value = "FIELD",  // after checking that this.vertices != 0
            target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;elementsToFill:I",
            ordinal = 0
        )
    )
    private int  endLastVertex(int elementsToFill) {
        if (elementsToFill == 0) {
            return 0;
        }

        long normalPtr = this.beginElement(VertexFormatElement.NORMAL);
        long tangentPtr = this.beginElement(CanPipe.VertexFormatElements.TANGENT);
        long materialFlagsPtr = this.beginElement(CanPipe.VertexFormatElements.MATERIAL_FLAGS);

        if (materialFlagsPtr != -1) {
            MemoryUtil.memPutByte(materialFlagsPtr, this.materialFlags);
        }

        this.canpipe_setAO(1.0F);

        boolean lastVertex = (this.vertices % this.mode.primitiveLength) == 0;
        if (!lastVertex || !(normalPtr != -1 || tangentPtr != -1)) {
            return this.elementsToFill;
        }

        // CanPipe.trap();

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
            z2 = this.canpipe_getPos(posPtr, offsetToFirstVertex+2, 2),
            x3 = this.canpipe_getPos(posPtr, offsetToFirstVertex+3, 0),
            y3 = this.canpipe_getPos(posPtr, offsetToFirstVertex+3, 1),
            z3 = this.canpipe_getPos(posPtr, offsetToFirstVertex+3, 2);

        Vector3f normal0 = NormalAndTangent.computeNormal(x0, y0, z0, x1, y1, z1, x2, y2, z2);

        if (normalPtr != -1) {
            if (
                this.mode.primitiveLength == 4 &&
                // not coplanar
                Math.abs(normal0.x*(x3-x1) + normal0.y*(y3-y1) + normal0.z*(z3-z1)) >= 0.0001F
            ) {
                Vector3f normal1 = NormalAndTangent.computeNormal(x2, y2, z2, x3, y3, z3, x0, y0, z0);

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
            Pair<Vector3f, Boolean> tangentPair = NormalAndTangent.computeTangent(
                normal0,
                x0, y0, z0, u0, v0,
                x1, y1, z1, u1, v1,
                x2, y2, z2, u2, v2
            );
            Vector3f tangent = tangentPair.getLeft();
            boolean inverseBitangent = tangentPair.getRight();
            for (int i = offsetToFirstVertex; i <= 0; ++i) {
                MemoryUtil.memPutByte(tangentPtr+i*this.vertexSize+0, normalIntValue(tangent.x));
                MemoryUtil.memPutByte(tangentPtr+i*this.vertexSize+1, normalIntValue(tangent.y));
                MemoryUtil.memPutByte(tangentPtr+i*this.vertexSize+2, normalIntValue(tangent.z));
                MemoryUtil.memPutByte(tangentPtr+i*this.vertexSize+3, normalIntValue(inverseBitangent ? -1.0F : 1.0F));
            }
        }

        return this.elementsToFill;
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

    @Override
    public void canpipe_setTangent(Consumer<TangentSetter> tangentSetterConsumer) {
        long ptr = this.beginElement(CanPipe.VertexFormatElements.TANGENT);
        if (ptr == -1) {
            return;
        }
        tangentSetterConsumer.accept((float x, float y, float z, boolean inverseBitangent) -> {
            MemoryUtil.memPutByte(ptr+0, normalIntValue(x));
            MemoryUtil.memPutByte(ptr+1, normalIntValue(y));
            MemoryUtil.memPutByte(ptr+2, normalIntValue(z));
            MemoryUtil.memPutByte(ptr+3, normalIntValue(inverseBitangent ? -1.0F : 1.0F));
        });
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

}
