package fewizz.canpipe.mixin;

import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipelines;

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
    protected static byte normalIntValue(float f) {return 0;}

    @Unique
    final Vector3f sharedTangent = new Vector3f(1.0F, 0.0F, 0.0F);

    @Unique
    int sharedMaterialIndex = -1;

    @Unique
    int sharedSpriteIndex = -1;

    @Unique
    boolean recomputeNormal = false;

    @Inject(method = "endLastVertex", at = @At("HEAD"))
    private void endLastVertex(CallbackInfo ci) {
        boolean requiresNormal = format.contains(VertexFormatElement.NORMAL);
        boolean normalIsNotSet = (elementsToFill & VertexFormatElement.NORMAL.mask()) != 0;

        if ((this.recomputeNormal && requiresNormal) || normalIsNotSet) {
            if (!this.mode.connectedPrimitives && this.vertices > 0 && (this.vertices % this.mode.primitiveLength) == 0) {
                int posOffset = this.offsetsByElement[VertexFormatElement.POSITION.id()];
                int normalOffset = this.offsetsByElement[VertexFormatElement.NORMAL.id()];
                long o = this.vertexPointer - this.vertexSize*(this.mode.primitiveLength - 1);
                Vector3f normal = Pipelines.computeNormal(
                    MemoryUtil.memGetFloat(o+this.vertexSize*0 + posOffset+0*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*0 + posOffset+1*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*0 + posOffset+2*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*1 + posOffset+0*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*1 + posOffset+1*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*1 + posOffset+2*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*2 + posOffset+0*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*2 + posOffset+1*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*2 + posOffset+2*Float.BYTES)
                );
                for (int i = 0; i < this.mode.primitiveLength; ++i) {
                    MemoryUtil.memPutByte(o+this.vertexSize*i+normalOffset+0, normalIntValue(normal.x));
                    MemoryUtil.memPutByte(o+this.vertexSize*i+normalOffset+1, normalIntValue(normal.y));
                    MemoryUtil.memPutByte(o+this.vertexSize*i+normalOffset+2, normalIntValue(normal.z));
                }
                this.elementsToFill &= ~VertexFormatElement.NORMAL.mask();
            }
            else if (normalIsNotSet) {
                setNormal(0.0F, 1.0F, 0.0F);
            }
        }

        if ((elementsToFill & CanPipe.VertexFormatElements.TANGENT.mask()) != 0) {
            if (!this.mode.connectedPrimitives && this.vertices > 0 && (this.vertices % this.mode.primitiveLength) == 0) {
                int posOffset = this.offsetsByElement[VertexFormatElement.POSITION.id()];
                int uvOffset = this.offsetsByElement[VertexFormatElement.UV0.id()];
                int tangentOffset = this.offsetsByElement[CanPipe.VertexFormatElements.TANGENT.id()];
                long o = this.vertexPointer - this.vertexSize*(this.mode.primitiveLength - 1);
                Vector3f tangent = Pipelines.computeTangent(
                    MemoryUtil.memGetFloat(o+this.vertexSize*0 + posOffset+0*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*0 + posOffset+1*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*0 + posOffset+2*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*0 + uvOffset+0*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*0 + uvOffset+1*Float.BYTES),

                    MemoryUtil.memGetFloat(o+this.vertexSize*1 + posOffset+0*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*1 + posOffset+1*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*1 + posOffset+2*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*1 + uvOffset+0*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*1 + uvOffset+1*Float.BYTES),

                    MemoryUtil.memGetFloat(o+this.vertexSize*2 + posOffset+0*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*2 + posOffset+1*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*2 + posOffset+2*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*2 + uvOffset+0*Float.BYTES),
                    MemoryUtil.memGetFloat(o+this.vertexSize*2 + uvOffset+1*Float.BYTES)
                ).normalize();
                for (int i = 0; i < this.mode.primitiveLength; ++i) {
                    MemoryUtil.memPutByte(o+this.vertexSize*i+tangentOffset+0, normalIntValue(tangent.x));
                    MemoryUtil.memPutByte(o+this.vertexSize*i+tangentOffset+1, normalIntValue(tangent.y));
                    MemoryUtil.memPutByte(o+this.vertexSize*i+tangentOffset+2, normalIntValue(tangent.z));
                    MemoryUtil.memPutByte(o+this.vertexSize*i+tangentOffset+3, (byte) 0);
                }
                this.elementsToFill &= ~CanPipe.VertexFormatElements.TANGENT.mask();
            }
            else {
                setSharedTangent(1.0F, 0.0F, 0.0F);
                inheritTangent();
            }
        }
        if ((elementsToFill & CanPipe.VertexFormatElements.AO.mask()) != 0) {
            setAO(1.0F);
        }
        if ((elementsToFill & CanPipe.VertexFormatElements.MATERIAL_INDEX.mask()) != 0) {
            setSharedMaterialIndex(-1);
            inheritMaterialIndex();
        }
        if ((elementsToFill & CanPipe.VertexFormatElements.SPRITE_INDEX.mask()) != 0) {
            setSharedSpriteIndex(-1);
            inheritSpriteIndex();
        }
    }

    @Override
    public void setAO(float ao) {
        long offset = this.beginElement(CanPipe.VertexFormatElements.AO);
        MemoryUtil.memPutFloat(offset, ao);
    }

    @Override
    public void setSharedSpriteIndex(int spriteIndex) {
        this.sharedSpriteIndex = spriteIndex;
    }

    @Override
    public void inheritSpriteIndex() {
        long offset = this.beginElement(CanPipe.VertexFormatElements.SPRITE_INDEX);
        MemoryUtil.memPutInt(offset, this.sharedSpriteIndex);
    }

    @Override
    public void setSharedMaterialIndex(int materialIndex) {
        this.sharedMaterialIndex = materialIndex;
    }

    @Override
    public void inheritMaterialIndex() {
        long offset = this.beginElement(CanPipe.VertexFormatElements.MATERIAL_INDEX);
        MemoryUtil.memPutInt(offset, this.sharedMaterialIndex);
    }

    @Override
    public void inheritTangent() {
        long offset = this.beginElement(CanPipe.VertexFormatElements.TANGENT);
        MemoryUtil.memPutByte(offset+0, normalIntValue(this.sharedTangent.x));
        MemoryUtil.memPutByte(offset+1, normalIntValue(this.sharedTangent.y));
        MemoryUtil.memPutByte(offset+2, normalIntValue(this.sharedTangent.z));
        MemoryUtil.memPutByte(offset+3, (byte) 0);
    }

    @Override
    public void setSharedTangent(float x, float y, float z) {
        this.sharedTangent.set(x, y, z);
    }

    @Override
    public void recomputeNormal(boolean recompute) {
        this.recomputeNormal = recompute;
    }

}
