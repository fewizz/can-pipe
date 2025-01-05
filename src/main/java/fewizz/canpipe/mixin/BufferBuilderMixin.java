package fewizz.canpipe.mixin;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.*;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.mixininterface.TextureAtlasSpriteExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
    int sharedMaterialIndex = -1;

    @Unique
    Supplier<TextureAtlasSprite> spriteSupplier = null;

    @Unique
    boolean recomputeNormal = false;

    @Inject(method = "endLastVertex", at = @At("HEAD"))
    private void endLastVertex(CallbackInfo ci) {
        boolean requiresNormal = format.contains(VertexFormatElement.NORMAL);
        boolean normalIsNotSet = (elementsToFill & VertexFormatElement.NORMAL.mask()) != 0;
        boolean lastVertex = !this.mode.connectedPrimitives && this.vertices != 0 && (this.vertices % this.mode.primitiveLength) == 0;
        int offsetToFirstVertex = -(this.mode.primitiveLength - 1);

        if ((this.recomputeNormal && requiresNormal) || normalIsNotSet) {
            if (lastVertex) {
                Vector3f normal = Pipelines.computeNormal(
                    canpipe_getPos(offsetToFirstVertex, 0),
                    canpipe_getPos(offsetToFirstVertex, 1),
                    canpipe_getPos(offsetToFirstVertex, 2),
                    canpipe_getPos(offsetToFirstVertex+1, 0),
                    canpipe_getPos(offsetToFirstVertex+1, 1),
                    canpipe_getPos(offsetToFirstVertex+1, 2),
                    canpipe_getPos(offsetToFirstVertex+2, 0),
                    canpipe_getPos(offsetToFirstVertex+2, 1),
                    canpipe_getPos(offsetToFirstVertex+2, 2)
                );
                int normalOffset = this.offsetsByElement[VertexFormatElement.NORMAL.id()];
                for (int i = 0; i < this.mode.primitiveLength; ++i) {
                    long o = this.vertexPointer + (long) this.vertexSize *(offsetToFirstVertex+i)+normalOffset;
                    MemoryUtil.memPutByte(o, normalIntValue(normal.x));
                    MemoryUtil.memPutByte(o+1, normalIntValue(normal.y));
                    MemoryUtil.memPutByte(o+2, normalIntValue(normal.z));
                }
                this.elementsToFill &= ~VertexFormatElement.NORMAL.mask();
            }
            else if (normalIsNotSet) {
                setNormal(0.0F, 1.0F, 0.0F);
            }
        }

        if ((elementsToFill & CanPipe.VertexFormatElements.TANGENT.mask()) != 0) {
            if (lastVertex) {
                Vector3f tangent = Pipelines.computeTangent(
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
                ).normalize();
                for (int i = 0; i < this.mode.primitiveLength; ++i) {
                    setTangentRaw(offsetToFirstVertex+i, tangent.x, tangent.y, tangent.z);
                }
            }
            else {
                setTangentRaw(0, 1.0F, 0.0F, 0.0F);
            }
            this.elementsToFill &= ~CanPipe.VertexFormatElements.TANGENT.mask();
        }

        if ((elementsToFill & CanPipe.VertexFormatElements.AO.mask()) != 0) {
            canpipe_setAO(1.0F);
        }
        if ((elementsToFill & CanPipe.VertexFormatElements.MATERIAL_INDEX.mask()) != 0) {
            long offset = this.beginElement(CanPipe.VertexFormatElements.MATERIAL_INDEX);
            MemoryUtil.memPutInt(offset, this.sharedMaterialIndex);
        }

        /*if ((elementsToFill & CanPipe.VertexFormatElements.SPRITE_INDEX.mask()) != 0) {
            if (lastVertex) {
                TextureAtlasSprite sprite = this.spriteSupplier != null ? this.spriteSupplier.get() : null;
                if (sprite != null) {
                    for (int i = 0; i < this.mode.primitiveLength; ++i) {
                        setSpriteRaw(offsetToFirstVertex+i, sprite);
                    }
                }
            }
            else {
                setSpriteRaw(0, null);
            }
            this.elementsToFill &= ~CanPipe.VertexFormatElements.SPRITE_INDEX.mask();
        }*/
    }

    @Inject(
        method = "setUv",
        at = @At("RETURN")
    )
    void onSetUv(float u, float v, CallbackInfoReturnable<VertexConsumer> cir) {
        if ((elementsToFill & CanPipe.VertexFormatElements.SPRITE_INDEX.mask()) == 0) {
            return;
        }

        boolean lastVertex = !this.mode.connectedPrimitives && this.vertices != 0 && (this.vertices % this.mode.primitiveLength) == 0;
        if (lastVertex) {
            int offsetToFirstVertex = -(this.mode.primitiveLength - 1);
            TextureAtlasSprite sprite = this.spriteSupplier != null ? this.spriteSupplier.get() : null;
            if (sprite != null) {
                for (int i = 0; i < this.mode.primitiveLength; ++i) {
                    setSpriteRaw(offsetToFirstVertex+i, sprite);
                }
            }
        }
        else {
            setSpriteRaw(0, null);
        }
        this.elementsToFill &= ~CanPipe.VertexFormatElements.SPRITE_INDEX.mask();
    }

    @Override
    public void canpipe_setAO(float ao) {
        long offset = this.beginElement(CanPipe.VertexFormatElements.AO);
        MemoryUtil.memPutFloat(offset, ao);
    }

    @Override
    public void canpipe_setSpriteSupplier(Supplier<TextureAtlasSprite> spriteSupplier) {
        this.spriteSupplier = spriteSupplier;
    }

    @Unique
    private void setSpriteRaw(int vertexOffset, TextureAtlasSprite sprite) {
        long spriteOffset = this.offsetsByElement[CanPipe.VertexFormatElements.SPRITE_INDEX.id()];
        long o = this.vertexPointer + (long) this.vertexSize * vertexOffset + spriteOffset;
        MemoryUtil.memPutInt(
            o,
            sprite != null ? ((TextureAtlasSpriteExtended) sprite).getIndex() : -1
        );
    }

    @Override
    public void canpipe_setSharedMaterialIndex(int materialIndex) {
        this.sharedMaterialIndex = materialIndex;
    }

    @Unique
    private void setTangentRaw(int vertexOffset, float x, float y, float z) {
        long tangentOffset = this.offsetsByElement[CanPipe.VertexFormatElements.TANGENT.id()];
        long o = this.vertexPointer + (long) this.vertexSize * vertexOffset + tangentOffset;
        MemoryUtil.memPutByte(o, normalIntValue(x));
        MemoryUtil.memPutByte(o+1, normalIntValue(y));
        MemoryUtil.memPutByte(o+2, normalIntValue(z));
        MemoryUtil.memPutByte(o+3, (byte) 0);
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

    @Unique
    private float canpipe_getPos(int vertexOffset, int element) {
        long posOffset = this.offsetsByElement[VertexFormatElement.POSITION.id()];
        long o = this.vertexPointer + (long) this.vertexSize*vertexOffset + posOffset;
        return MemoryUtil.memGetFloat(o + (long) element*Float.BYTES);
    }
}
