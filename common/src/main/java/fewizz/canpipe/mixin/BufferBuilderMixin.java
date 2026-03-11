package fewizz.canpipe.mixin;

import java.util.function.Supplier;

import org.apache.commons.lang3.mutable.MutableObject;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.helpers.NormalAndTangent;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.mixininterface.TextureAtlasSpriteExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements VertexConsumerExtended {

    @Shadow private int vertices;
    @Shadow private int elementsToFill;
    @Shadow @Final private VertexFormat.Mode mode;
    @Shadow @Final private ByteBufferBuilder buffer;
    @Shadow @Final public VertexFormat format;
    @Shadow @Final private boolean fastFormat;
    @Shadow @Final private int vertexSize;
    @Shadow @Final private int[] offsetsByElement;
    @Shadow private long vertexPointer = -1L;

    @Shadow private long beginElement(VertexFormatElement vertexFormatElement) { return -1; }
    @Shadow private static byte normalIntValue(float f) { return 0; }

    @Unique private MaterialMap canpipe_materialMap = null;
    @Unique private byte canpipe_materialFlags = 0;
    @Unique private Supplier<TextureAtlasSprite> canpipe_spriteSupplier = null;
    @Unique private boolean canpipe_recomputeNormal = false;
    @Unique private Float canpipe_aoPending = null;
    @Unique private Identifier canpipe_textureIdentifier = null;

    @Unique @Final private int canpipe_aoOffset;
    @Unique @Final private int canpipe_uv0Offset;
    @Unique @Final private int canpipe_positionOffset;
    @Unique @Final private int canpipe_spriteIndexOffset;
    @Unique @Final private int canpipe_materialIndexOffset;
    @Unique @Final private int canpipe_materialFlagsOffset;
    @Unique @Final private int canpipe_normalOffset;
    @Unique @Final private int canpipe_tangentOffset;

    @Override public VertexFormat canpipe_getVertexFormat() { return this.format; }

    private static byte canpipe_normalIntValueWithoutClamp(float value) {
        return (byte) Math.fma(value, 127.5F, -1.0F / 255.0F);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    void onInit(CallbackInfo ci) {
        this.canpipe_aoOffset = this.format.getOffset(CanPipe.VertexFormatElements.AO);
        this.canpipe_uv0Offset = this.format.getOffset(VertexFormatElement.UV0);
        this.canpipe_positionOffset = this.format.getOffset(VertexFormatElement.POSITION);  // Should be 0
        this.canpipe_spriteIndexOffset = this.format.getOffset(CanPipe.VertexFormatElements.SPRITE_INDEX);
        this.canpipe_materialIndexOffset = this.format.getOffset(CanPipe.VertexFormatElements.MATERIAL_INDEX);
        this.canpipe_materialFlagsOffset = this.format.getOffset(CanPipe.VertexFormatElements.MATERIAL_FLAGS);
        this.canpipe_normalOffset = this.format.getOffset(VertexFormatElement.NORMAL);
        this.canpipe_tangentOffset = this.format.getOffset(CanPipe.VertexFormatElements.TANGENT);

        if (this.canpipe_materialIndexOffset == -1 && this.canpipe_materialFlagsOffset != -1) {
            throw new RuntimeException("in_materialIndex should be enabled with in_materialFlags");
        }

        if (this.canpipe_normalOffset == -1 && this.canpipe_tangentOffset != -1) {
            throw new RuntimeException("in_tangent should be enabled with in_tangent");
        }
    }

    private void canpipe_setNormalAndTangent(long normalPtr, float normalX, float normalY, float normalZ, long tangentPtr) {
        if (normalPtr == -1 && tangentPtr == -1) { return; }

        int offsetToFirstVertex = -(this.mode.primitiveLength - 1);

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

            if (this.mode.primitiveLength == 4) {
                x3 = this.canpipe_getPosX(offsetToFirstVertex+3);
                y3 = this.canpipe_getPosY(offsetToFirstVertex+3);
                z3 = this.canpipe_getPosZ(offsetToFirstVertex+3);
            }

            if (
                this.mode.primitiveLength == 4 &&
                // not coplanar
                Math.abs(normal0.x*(x3-x1) + normal0.y*(y3-y1) + normal0.z*(z3-z1)) >= 0.0001F
            ) {
                Vector3f normal1 = NormalAndTangent.computeNormal(x2, y2, z2, x3, y3, z3, x0, y0, z0);

                Vector3f mid = new Vector3f(normal0).add(normal1).normalize();

                int i = offsetToFirstVertex;
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+0, canpipe_normalIntValueWithoutClamp(mid.x));
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+1, canpipe_normalIntValueWithoutClamp(mid.y));
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+2, canpipe_normalIntValueWithoutClamp(mid.z));

                i += 1;
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+0, canpipe_normalIntValueWithoutClamp(normal0.x));
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+1, canpipe_normalIntValueWithoutClamp(normal0.y));
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+2, canpipe_normalIntValueWithoutClamp(normal0.z));

                i += 1;
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+0, canpipe_normalIntValueWithoutClamp(mid.x));
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+1, canpipe_normalIntValueWithoutClamp(mid.y));
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+2, canpipe_normalIntValueWithoutClamp(mid.z));

                i += 1;
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+0, canpipe_normalIntValueWithoutClamp(normal1.x));
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+1, canpipe_normalIntValueWithoutClamp(normal1.y));
                MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+2, canpipe_normalIntValueWithoutClamp(normal1.z));
            } else {
                for (int i = offsetToFirstVertex; i <= 0; ++i) {
                    MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+0, canpipe_normalIntValueWithoutClamp(normal0.x));
                    MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+1, canpipe_normalIntValueWithoutClamp(normal0.y));
                    MemoryUtil.memPutByte(normalPtr+this.vertexSize*i+2, canpipe_normalIntValueWithoutClamp(normal0.z));
                }
            }
        }

        if (tangentPtr != -1) {
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
                MemoryUtil.memPutByte(tangentPtr+i*this.vertexSize+0, canpipe_normalIntValueWithoutClamp(tangent.x));
                MemoryUtil.memPutByte(tangentPtr+i*this.vertexSize+1, canpipe_normalIntValueWithoutClamp(tangent.y));
                MemoryUtil.memPutByte(tangentPtr+i*this.vertexSize+2, canpipe_normalIntValueWithoutClamp(tangent.z));
                MemoryUtil.memPutByte(tangentPtr+i*this.vertexSize+3, canpipe_normalIntValueWithoutClamp(inverseBitangent ? -1.0F : 1.0F));
            }
        }
    }

    private void canpipe_setSpriteAndMaterial(long spriteIndexPtr, long materialIndexPtr, long materialFlagsPtr) {
        if (spriteIndexPtr == -1 && materialIndexPtr == -1 && materialFlagsPtr == -1) { return; }

        int offsetToFirstVertex = -(this.mode.primitiveLength - 1);

        TextureAtlasSprite sprite = this.canpipe_spriteSupplier != null ? this.canpipe_spriteSupplier.get() : null;

        if (spriteIndexPtr != -1) {
            int index = sprite != null ? ((TextureAtlasSpriteExtended) sprite).getIndex() : -1;
            for (int i = offsetToFirstVertex; i <= 0; ++i) {
                MemoryUtil.memPutInt(spriteIndexPtr + i*this.vertexSize, index);
            }
        }

        if (materialIndexPtr != -1) {
            Material material = null;

            if (this.canpipe_materialMap != null) {
                if (this.canpipe_textureIdentifier != null) {
                    material = this.canpipe_materialMap.spriteMap.get(this.canpipe_textureIdentifier);
                }

                if (material == null && this.canpipe_materialMap.spriteMap != null && sprite != null) {
                    Minecraft mc = Minecraft.getInstance();

                    MutableObject<TextureAtlas> atlas = new MutableObject<>();
                    mc.getAtlasManager().forEach((loc, possibleAtlas) -> {
                        if (atlas.get() == null && possibleAtlas.location().equals(sprite.atlasLocation())) {
                            atlas.setValue(possibleAtlas);
                        }
                    });

                    for (var kv : this.canpipe_materialMap.spriteMap.entrySet()) {
                        if (atlas.get().getSprite(kv.getKey()) == sprite) {
                            material = kv.getValue();
                        }
                    }
                }

                if (material == null) {
                    material = canpipe_materialMap.defaultMaterial;
                }
            }

            int materialIndex = material != null ? Materials.id(material) : -1;

            if (material != null && material.disableAO) { this.canpipe_materialFlags |= 1 << 1; }
            else  { this.canpipe_materialFlags &= ~(1 << 1); }

            if (material != null && material.disableDiffuse) { this.canpipe_materialFlags |= 1 << 2; }
            else  { this.canpipe_materialFlags &= ~(1 << 2); }

            for (int i = offsetToFirstVertex; i <= 0; ++i) {
                MemoryUtil.memPutShort(materialIndexPtr+i*this.vertexSize, (short) materialIndex);
                MemoryUtil.memPutByte(materialFlagsPtr+i*this.vertexSize, this.canpipe_materialFlags);
            }
        }
    }

    @ModifyVariable(method = "<init>", at = @At("STORE"), ordinal = 0)  // if format is ENTITY
    private boolean onEntityFormatSet(boolean value) {
        return value || this.format == CanPipe.VertexFormats.ENTITY || this.format == CanPipe.VertexFormats.ENTITY_SHADOW;
    }

    @ModifyVariable(method = "<init>", at = @At("STORE"), ordinal = 1)  // if format is BLOCK
    private boolean onBlockFormatSet(boolean value) {
        return value || this.format == CanPipe.VertexFormats.BLOCK;
    }

    @Inject(method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("RETURN"))
    private void onAddVertex(CallbackInfoReturnable<VertexConsumer> cir) {
        var ptr = this.beginElement(CanPipe.VertexFormatElements.AO);
        if (ptr != -1) {
            float ao = this.canpipe_aoPending != null ? this.canpipe_aoPending : 1.0F;
            MemoryUtil.memPutByte(this.vertexPointer + this.canpipe_aoOffset, (byte)(Math.clamp(ao, 0.0F, 1.0F)*255.0F));
            this.canpipe_aoPending = null;
        }
    }

    @Inject(method = "endLastVertex", at = @At("HEAD"))
    private void endLastVertex(CallbackInfo ci) {
        if (this.vertices == 0) {
            return;
        }

        long normalPtr = this.beginElement(VertexFormatElement.NORMAL);
        long tangentPtr = this.beginElement(CanPipe.VertexFormatElements.TANGENT);

        boolean lastVertex = (this.vertices % this.mode.primitiveLength) == 0;

        if (lastVertex) {
            canpipe_setNormalAndTangent(normalPtr, -1, -1, -1, tangentPtr);
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
        long materialFlagsPtr = this.beginElement(CanPipe.VertexFormatElements.MATERIAL_FLAGS);

        boolean lastVertex = (this.vertices % this.mode.primitiveLength) == 0;
        if (lastVertex) {
            canpipe_setSpriteAndMaterial(spriteIndexPtr, materialIndexPtr, materialFlagsPtr);
        }
    }

    @Inject(method = "addVertex(FFFIFFIIFFF)V", at = @At("RETURN"))
    private void onAddVertexBulk(
        CallbackInfo ci, @Local(ordinal = 5) float normalX, @Local(ordinal = 6) float normalY, @Local(ordinal = 7) float normalZ
    ) {
        if (!this.fastFormat) { return; }  // Because I don't know how to Mixin

        if (this.canpipe_aoOffset != -1) {
            float ao = this.canpipe_aoPending != null ? this.canpipe_aoPending : 1.0F;
            MemoryUtil.memPutByte(this.vertexPointer + this.canpipe_aoOffset, (byte)(Math.clamp(ao, 0.0F, 1.0F)*255.0F));
            this.canpipe_aoPending = null;
        }

        boolean lastVertex = (this.vertices % this.mode.primitiveLength) == 0;
        if (lastVertex) {
            this.canpipe_setSpriteAndMaterial(
                this.canpipe_spriteIndexOffset != -1 ? this.vertexPointer + this.canpipe_spriteIndexOffset : -1,
                this.canpipe_materialIndexOffset != -1 ? this.vertexPointer + this.canpipe_materialIndexOffset : -1,
                this.canpipe_materialFlagsOffset != -1 ? this.vertexPointer + this.canpipe_materialFlagsOffset : -1
            );

            this.canpipe_setNormalAndTangent(
                this.canpipe_recomputeNormal && this.canpipe_normalOffset != -1 ? this.vertexPointer + this.canpipe_normalOffset : -1,
                normalX, normalY, normalZ,
                this.canpipe_tangentOffset != -1 ? this.vertexPointer + this.canpipe_tangentOffset : -1
            );
        }
    }

    @ModifyExpressionValue(
        method = "setNormal",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;beginElement(Lcom/mojang/blaze3d/vertex/VertexFormatElement;)J"
        )
    )
    private long onSetNormal(long normalPtr, float normalX, float normalY, float normalZ) {
        boolean lastVertex = (this.vertices % this.mode.primitiveLength) == 0;

        if (lastVertex) {
            long tangentPtr = this.beginElement(CanPipe.VertexFormatElements.TANGENT);
            canpipe_setNormalAndTangent(this.canpipe_recomputeNormal ? normalPtr : -1, normalX, normalY, normalZ, tangentPtr);
        }

        return this.canpipe_recomputeNormal ? -1 : normalPtr;
    }

    @Override
    public void canpipe_setPendingAO(float ao) {
        this.canpipe_aoPending = ao;
    }

    @Override
    public void canpipe_setScopedSpriteSupplier(Supplier<TextureAtlasSprite> spriteSupplier) {
        this.canpipe_spriteSupplier = spriteSupplier;
    }

    @Override
    public void canpipe_setScopedMaterialMap(MaterialMap materialmap) {
        this.canpipe_materialMap = materialmap;
    }

    @Override
    public void canpipe_setScopedGlint(boolean glint) {
        if (glint) { this.canpipe_materialFlags |=   1 << 0;  }
        else       { this.canpipe_materialFlags &= ~(1 << 0); }
    }

    @Override
    public void canpipe_forceNormalRecomputation(boolean recompute) {
        this.canpipe_recomputeNormal = recompute;
    }

    @Override
    public void canpipe_setScopedTextureIdentifier(Identifier textureIdentifier) {
        this.canpipe_textureIdentifier = textureIdentifier;
    }

    @Override public float canpipe_getU(int vertexOffset) {
        return MemoryUtil.memGetFloat(this.vertexPointer + this.canpipe_uv0Offset + vertexOffset*this.vertexSize + 0*Float.BYTES);
    }

    @Override public float canpipe_getV(int vertexOffset) {
        return MemoryUtil.memGetFloat(this.vertexPointer + this.canpipe_uv0Offset + vertexOffset*this.vertexSize + 1*Float.BYTES);
    }

    private final float canpipe_getPosX(int vertexOffset) {
        return MemoryUtil.memGetFloat(this.vertexPointer + this.canpipe_positionOffset + vertexOffset*this.vertexSize + 0*Float.BYTES);
    }

    private final float canpipe_getPosY(int vertexOffset) {
        return MemoryUtil.memGetFloat(this.vertexPointer + this.canpipe_positionOffset + vertexOffset*this.vertexSize + 1*Float.BYTES);
    }

    private final float canpipe_getPosZ(int vertexOffset) {
        return MemoryUtil.memGetFloat(this.vertexPointer + this.canpipe_positionOffset + vertexOffset*this.vertexSize + 2*Float.BYTES);
    }

}
