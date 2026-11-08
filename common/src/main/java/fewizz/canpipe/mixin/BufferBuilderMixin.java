package fewizz.canpipe.mixin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.mixininterface.TextureAtlasSpriteExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.util.NormalAndTangent;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements VertexConsumerExtended {

    @Shadow @Final private static boolean IS_LITTLE_ENDIAN;
    @Shadow private int vertices;
    @Shadow @Final private PrimitiveTopology primitiveTopology;
    @Shadow @Final private int vertexSize;
    @Shadow @Final private @Nullable VertexFormatElement[] elements;
    @Shadow @Final @Mutable private static String[] elementNames;
    @Shadow private long vertexPointer = -1L;

    @Shadow private long beginElement(int semanticID) { return -1; }
    @Shadow private long beginVertex() { return -1; }
    @Shadow private static void putVec3f(final long pointer, final float x, final float y, final float z) {}
    @Shadow private static void putRgba(final long pointer, final int argb) {}
    @Shadow private static void putPackedUv(final long pointer, final int packedUv) {}

    @Unique private short canpipe_pendingMaterialIndex = -1;
    @Unique private Function<TextureAtlasSprite, Material> canpipe_materialSupplier = null;
    @Unique private boolean canpipe_glint = false;
    @Unique private boolean canpipe_entityGlint = false;
    @Unique private int canpipe_pendingSpriteIndex = -1;
    @Unique private Supplier<TextureAtlasSprite> canpipe_spriteSupplier = null;
    @Unique private boolean canpipe_recomputeNormal = false;
    @Unique private Float canpipe_aoPending = null;
    @Unique private boolean canpipe_fastPathEntityShadow = false;

    @Unique private int canpipe_aoOffset;
    @Unique private int canpipe_uv0Offset;
    @Unique private int canpipe_positionOffset;
    @Unique private int canpipe_spriteIndexOffset;
    @Unique private int canpipe_materialIndexOffset;

    @Unique private static int CANPIPE_MATERIAL_FLAGS_SEMANTIC_ID;
    @Unique private static int CANPIPE_MATERIAL_INDEX_SEMANTIC_ID;
    @Unique private static int CANPIPE_SPRITE_INDEX_SEMANTIC_ID;
    @Unique private static int CANPIPE_TANGENT_SEMANTIC_ID;
    @Unique private static int CANPIPE_AO_SEMANTIC_ID;

    @Override public boolean canpipe_hasMaterialIndex() { return this.canpipe_materialIndexOffset != -1; }
    @Override public boolean canpipe_hasSpriteIndex() { return this.canpipe_spriteIndexOffset != -1; }
    @Override public boolean canpipe_hasAO() { return this.canpipe_aoOffset != -1; }

    @Unique
    private static byte canpipe_normalIntValueWithoutClamp(float value) {
        return (byte) Math.fma(value, 127.5F, -1.0F / 255.0F);
    }

    @Inject(method = "<clinit>", at = @At("RETURN"))
    static private void extendFormats(CallbackInfo ci) {
        var list = new ArrayList<>(Arrays.asList(elementNames));
        CANPIPE_MATERIAL_FLAGS_SEMANTIC_ID = list.size();
        list.add(CanPipe.VertexFormats.MATERIAL_FLAGS_ATTRIBUTE_NAME);
        CANPIPE_MATERIAL_INDEX_SEMANTIC_ID = list.size();
        list.add(CanPipe.VertexFormats.MATERIAL_INDEX_ATTRIBUTE_NAME);
        CANPIPE_SPRITE_INDEX_SEMANTIC_ID = list.size();
        list.add(CanPipe.VertexFormats.SPRITE_INDEX_ATTRIBUTE_NAME);
        CANPIPE_TANGENT_SEMANTIC_ID = list.size();
        list.add(CanPipe.VertexFormats.TANGENT_ATTRIBUTE_NAME);
        CANPIPE_AO_SEMANTIC_ID = list.size();
        list.add(CanPipe.VertexFormats.AO_ATTRIBUTE_NAME);
        elementNames = list.toArray(new String[]{});
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    void onInit(CallbackInfo ci, @Local VertexFormat format) {
        Int2IntFunction getOffset = (int semanticID) -> {
            var element = this.elements[semanticID];
            return element != null ? element.offset() : -1;
        };

        this.canpipe_aoOffset = getOffset.apply(CANPIPE_AO_SEMANTIC_ID);
        this.canpipe_uv0Offset = getOffset.apply(BufferBuilder.UV0_SEMANTIC_ID);
        this.canpipe_positionOffset = getOffset.apply(BufferBuilder.POSITION_SEMANTIC_ID);
        this.canpipe_spriteIndexOffset = getOffset.apply(CANPIPE_SPRITE_INDEX_SEMANTIC_ID);
        this.canpipe_materialIndexOffset = getOffset.apply(CANPIPE_MATERIAL_INDEX_SEMANTIC_ID);

        boolean hasMaterialIndex = this.canpipe_materialIndexOffset != -1;
        boolean hasMaterialFlags = this.elements[CANPIPE_MATERIAL_FLAGS_SEMANTIC_ID] != null;
        boolean hasNormal = this.elements[BufferBuilder.NORMAL_SEMANTIC_ID] != null;
        boolean hasTangent = this.elements[CANPIPE_TANGENT_SEMANTIC_ID] != null;

        if (hasMaterialFlags && !hasMaterialIndex) {
            throw new RuntimeException("in_materialIndex should be enabled with in_materialFlags");
        }

        if (hasTangent && !hasNormal) {
            throw new RuntimeException("Normal should be enabled with in_tangent");
        }

        this.canpipe_fastPathEntityShadow = format == CanPipe.VertexFormats.ENTITY_SHADOW;
    }

    @Unique
    private void canpipe_setNormalAndTangent(long normalPtr, float normalX, float normalY, float normalZ, long tangentPtr) {
        if (normalPtr == -1 && tangentPtr == -1) { return; }

        int offsetToFirstVertex = -(this.primitiveTopology.primitiveLength - 1);

        float
            x0 = this.canpipe_getPosX(offsetToFirstVertex+0),
            y0 = this.canpipe_getPosY(offsetToFirstVertex+0),
            z0 = this.canpipe_getPosZ(offsetToFirstVertex+0),
            x1 = this.canpipe_getPosX(offsetToFirstVertex+1),
            y1 = this.canpipe_getPosY(offsetToFirstVertex+1),
            z1 = this.canpipe_getPosZ(offsetToFirstVertex+1),
            x2 = this.canpipe_getPosX(offsetToFirstVertex+2),
            y2 = this.canpipe_getPosY(offsetToFirstVertex+2),
            z2 = this.canpipe_getPosZ(offsetToFirstVertex+2);

        Vector3f normal0;

        if (normalPtr == -1) {
            normal0 = new Vector3f(normalX, normalY, normalZ);
        }
        else {
            normal0 = NormalAndTangent.computeNormal(x0, y0, z0, x1, y1, z1, x2, y2, z2);

            float x3 = 0, y3 = 0, z3 = 0;

            if (this.primitiveTopology.primitiveLength == 4) {
                x3 = this.canpipe_getPosX(offsetToFirstVertex+3);
                y3 = this.canpipe_getPosY(offsetToFirstVertex+3);
                z3 = this.canpipe_getPosZ(offsetToFirstVertex+3);
            }

            if (
                this.primitiveTopology.primitiveLength == 4 &&
                // not coplanar
                Math.abs(normal0.x*(x3-x1) + normal0.y*(y3-y1) + normal0.z*(z3-z1)) >= 0.0001F
            ) {
                Vector3f normal1 = NormalAndTangent.computeNormal(x2, y2, z2, x3, y3, z3, x0, y0, z0);

                Vector3f mid = new Vector3f(normal0).add(normal1).normalize();

                int i = offsetToFirstVertex;
                MemoryUtil.memCopy(new byte[] {
                    canpipe_normalIntValueWithoutClamp(mid.x),
                    canpipe_normalIntValueWithoutClamp(mid.y),
                    canpipe_normalIntValueWithoutClamp(mid.z)
                }, normalPtr+this.vertexSize*i);

                i += 1;
                MemoryUtil.memCopy(new byte[] {
                    canpipe_normalIntValueWithoutClamp(normal0.x),
                    canpipe_normalIntValueWithoutClamp(normal0.y),
                    canpipe_normalIntValueWithoutClamp(normal0.z)
                }, normalPtr+this.vertexSize*i);

                i += 1;
                MemoryUtil.memCopy(new byte[] {
                    canpipe_normalIntValueWithoutClamp(mid.x),
                    canpipe_normalIntValueWithoutClamp(mid.y),
                    canpipe_normalIntValueWithoutClamp(mid.z)
                }, normalPtr+this.vertexSize*i);

                i += 1;
                MemoryUtil.memCopy(new byte[] {
                    canpipe_normalIntValueWithoutClamp(normal1.x),
                    canpipe_normalIntValueWithoutClamp(normal1.y),
                    canpipe_normalIntValueWithoutClamp(normal1.z)
                }, normalPtr+this.vertexSize*i);
            } else {
                for (int i = offsetToFirstVertex; i <= 0; ++i) {
                    MemoryUtil.memCopy(new byte[] {
                        canpipe_normalIntValueWithoutClamp(normal0.x),
                        canpipe_normalIntValueWithoutClamp(normal0.y),
                        canpipe_normalIntValueWithoutClamp(normal0.z)
                    }, normalPtr+this.vertexSize*i);
                }
            }
        }

        if (tangentPtr != -1) {
            if (this.primitiveTopology.primitiveLength != 4) {
                throw new RuntimeException("Can't compute tangent for non-quad ("+this.primitiveTopology.name()+") primitive topology");
            }

            float
                u0 = this.canpipe_getU(offsetToFirstVertex+0),
                v0 = this.canpipe_getV(offsetToFirstVertex+0),
                u1 = this.canpipe_getU(offsetToFirstVertex+1),
                v1 = this.canpipe_getV(offsetToFirstVertex+1),
                u2 = this.canpipe_getU(offsetToFirstVertex+2),
                v2 = this.canpipe_getV(offsetToFirstVertex+2);
            Pair<Vector3f, Boolean> tangentPair = NormalAndTangent.computeTangent(
                normal0,
                x0, y0, z0, u0, v0,
                x1, y1, z1, u1, v1,
                x2, y2, z2, u2, v2
            );
            Vector3f tangent = tangentPair.getLeft();
            boolean inverseBitangent = tangentPair.getRight();
            for (int i = offsetToFirstVertex; i <= 0; ++i) {
                MemoryUtil.memCopy(new byte[] {
                    canpipe_normalIntValueWithoutClamp(tangent.x),
                    canpipe_normalIntValueWithoutClamp(tangent.y),
                    canpipe_normalIntValueWithoutClamp(tangent.z),
                    canpipe_normalIntValueWithoutClamp(inverseBitangent ? -1.0F : 1.0F)
                }, tangentPtr+i*this.vertexSize);
            }
        }
    }

    @Unique
    private void canpipe_setSpriteAndMaterial(long spriteIndexPtr, long materialIndexPtr, long materialFlagsPtr) {
        if (spriteIndexPtr == -1 && materialIndexPtr == -1 && materialFlagsPtr == -1) { return; }

        int offsetToFirstVertex = -(this.primitiveTopology.primitiveLength - 1);

        TextureAtlasSprite sprite = this.canpipe_spriteSupplier != null ? this.canpipe_spriteSupplier.get() : null;

        if (spriteIndexPtr != -1) {
            if (this.canpipe_pendingSpriteIndex == -1 && sprite != null) {
                this.canpipe_pendingSpriteIndex = ((TextureAtlasSpriteExtended) sprite).canpipe_getIndex();
            }
            for (int i = offsetToFirstVertex; i <= 0; ++i) {
                MemoryUtil.memPutInt(spriteIndexPtr + i*this.vertexSize, this.canpipe_pendingSpriteIndex);
            }
            this.canpipe_pendingSpriteIndex = -1;
        }

        if (materialIndexPtr != -1) {
            Material material = null;

            if (this.canpipe_pendingMaterialIndex != -1) {
                material = Materials.get(canpipe_pendingMaterialIndex);
            }
            else if (this.canpipe_materialSupplier != null) {
                material = this.canpipe_materialSupplier.apply(sprite);
                if (material != null) {
                    this.canpipe_pendingMaterialIndex = material.index();
                }
            }

            byte materialFlags = (byte) 0;

            materialFlags |= (byte) (this.canpipe_glint ? 1 : 0) << 0;
            materialFlags |= (byte) (this.canpipe_entityGlint ? 1 : 0) << 1;

            if (material != null) {
                materialFlags |= (byte) (material.disableAO() ? 1 : 0) << 2;
                materialFlags |= (byte) (material.disableDiffuse() ? 1 : 0) << 3;
                materialFlags |= (byte) (material.disableColorIndex() ? 1 : 0) << 4;
                materialFlags |= (byte) (material.emissive() ? 1 : 0) << 5;
            }

            for (int i = offsetToFirstVertex; i <= 0; ++i) {
                MemoryUtil.memPutShort(materialIndexPtr+i*this.vertexSize, this.canpipe_pendingMaterialIndex);
                MemoryUtil.memPutByte(materialFlagsPtr+(i*this.vertexSize), materialFlags);
            }

            this.canpipe_pendingMaterialIndex = -1;
        }
    }

    @Inject(method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("RETURN"))
    private void onAddVertex(CallbackInfoReturnable<VertexConsumer> cir) {
        var ptr = this.beginElement(CANPIPE_AO_SEMANTIC_ID);
        if (ptr != -1) {
            float ao = this.canpipe_aoPending != null ? this.canpipe_aoPending : 1.0F;
            MemoryUtil.memPutByte(this.vertexPointer + this.canpipe_aoOffset, (byte)(Math.clamp(ao, 0.0F, 1.0F)*255.0F));
            this.canpipe_aoPending = null;
        }
        if (this.canpipe_recomputeNormal) {
            this.setNormal(-1, -1, -1);
        }
    }

    @ModifyExpressionValue(
        method = "setNormal",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;beginElement(I)J"
        )
    )
    private long onSetNormal(long normalPtr, float x, float y, float z) {
        if (normalPtr != -1) {
            boolean lastVertex = (this.vertices % this.primitiveTopology.primitiveLength) == 0;
            long tangentPtr = this.beginElement(CANPIPE_TANGENT_SEMANTIC_ID);

            if (lastVertex) {
                this.canpipe_setNormalAndTangent(this.canpipe_recomputeNormal ? normalPtr : -1, x, y, z, tangentPtr);
            }
        }

        return this.canpipe_recomputeNormal ? -1 : normalPtr;
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
        long spriteIndexPtr = this.beginElement(CANPIPE_SPRITE_INDEX_SEMANTIC_ID);
        long materialIndexPtr = this.beginElement(CANPIPE_MATERIAL_INDEX_SEMANTIC_ID);
        long materialFlagsPtr = this.beginElement(CANPIPE_MATERIAL_FLAGS_SEMANTIC_ID);

        boolean lastVertex = (this.vertices % this.primitiveTopology.primitiveLength) == 0;
        if (lastVertex) {
            this.canpipe_setSpriteAndMaterial(spriteIndexPtr, materialIndexPtr, materialFlagsPtr);
        }
    }

    @Inject(
        method = "addVertex(FFFIFFIIFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex"
        ),
        cancellable = true
    )
    private void onAddVertexFull(
        float x, float y, float z,
        int color, float u, float v,
        int overlayCoords, int lightCoords,
        float nx, float ny, float nz,
        CallbackInfo ci
    ) {
        if (!this.canpipe_fastPathEntityShadow) {
            return;
        }

        int spriteIndex = -1;

        if (this.canpipe_pendingSpriteIndex != -1) {
            spriteIndex = this.canpipe_pendingSpriteIndex;
        }
        else if (this.canpipe_spriteSupplier != null) {
            TextureAtlasSprite sprite = this.canpipe_spriteSupplier.get();

            if (sprite != null) {
                this.canpipe_pendingSpriteIndex = ((TextureAtlasSpriteExtended) sprite).canpipe_getIndex();
            }
        }

        int abgr = ARGB.toABGR(color /* argb */);

        long pointer = this.beginVertex();

        MemoryUtil.memCopy(new int[] {
            Float.floatToRawIntBits(x), Float.floatToRawIntBits(y), Float.floatToRawIntBits(z),
            IS_LITTLE_ENDIAN ? abgr : Integer.reverseBytes(abgr),   // from BufferBuilder.putRgba, no idea why abgr tho
            Float.floatToRawIntBits(u), Float.floatToRawIntBits(v),
            IS_LITTLE_ENDIAN ? overlayCoords : ((overlayCoords << 16) | (overlayCoords >>> 16)),
            spriteIndex
        }, pointer);

        ci.cancel();
    }

    @Override
    public void canpipe_setPendingAO(float ao) {
        this.canpipe_aoPending = ao;
    }

    @Override
    public void canpipe_setPendingSpriteIndex(int spriteIndex) {
        this.canpipe_pendingSpriteIndex = spriteIndex;
    }

    @Override
    public void canpipe_setScopedSpriteSupplier(Supplier<TextureAtlasSprite> spriteSupplier) {
        this.canpipe_spriteSupplier = spriteSupplier;
    }

    @Override
    public void canpipe_setPendingMaterialIndex(short materialIndex) {
        this.canpipe_pendingMaterialIndex = materialIndex;
    }

    @Override
    public void canpipe_setScopedMaterialSupplier(Function<TextureAtlasSprite, Material> materialSupplier) {
        this.canpipe_materialSupplier = materialSupplier;
    }

    @Override
    public void canpipe_setScopedGlint(boolean glint) {
        this.canpipe_glint = glint;
    }

    @Override
    public void canpipe_setScopedEntityGlint(boolean glint) {
        this.canpipe_entityGlint = glint;
    }

    @Override
    public void canpipe_forceNormalRecomputation(boolean recompute) {
        this.canpipe_recomputeNormal = recompute;
    }

    @Override public float canpipe_getU(int vertexOffset) {
        return MemoryUtil.memGetFloat(this.vertexPointer + this.canpipe_uv0Offset + vertexOffset*this.vertexSize + 0*Float.BYTES);
    }

    @Override public float canpipe_getV(int vertexOffset) {
        return MemoryUtil.memGetFloat(this.vertexPointer + this.canpipe_uv0Offset + vertexOffset*this.vertexSize + 1*Float.BYTES);
    }

    @Unique private float canpipe_getPosX(int vertexOffset) {
        return MemoryUtil.memGetFloat(this.vertexPointer + this.canpipe_positionOffset + vertexOffset*this.vertexSize + 0*Float.BYTES);
    }

    @Unique private float canpipe_getPosY(int vertexOffset) {
        return MemoryUtil.memGetFloat(this.vertexPointer + this.canpipe_positionOffset + vertexOffset*this.vertexSize + 1*Float.BYTES);
    }

    @Unique private float canpipe_getPosZ(int vertexOffset) {
        return MemoryUtil.memGetFloat(this.vertexPointer + this.canpipe_positionOffset + vertexOffset*this.vertexSize + 2*Float.BYTES);
    }

}
