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
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import fewizz.canpipe.CanPipeVertexFormatElements;
import fewizz.canpipe.Pipelines;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;

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

    @Inject(method = "endLastVertex", at = @At("HEAD"))
    private void endLastVertex(CallbackInfo ci) {
        if ((elementsToFill & VertexFormatElement.NORMAL.mask()) != 0) {
            if (this.mode == Mode.QUADS && vertices % 4 == 3) {
                int posOffset = this.offsetsByElement[VertexFormatElement.POSITION.id()];
                int normalOffset = this.offsetsByElement[VertexFormatElement.NORMAL.id()];
                long o = this.vertexPointer - this.vertexSize*3;
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
                MemoryUtil.memPutByte(o+this.vertexSize*0+normalOffset+0, normalIntValue(normal.x));
                MemoryUtil.memPutByte(o+this.vertexSize*0+normalOffset+1, normalIntValue(normal.y));
                MemoryUtil.memPutByte(o+this.vertexSize*0+normalOffset+2, normalIntValue(normal.z));
                MemoryUtil.memPutByte(o+this.vertexSize*1+normalOffset+0, normalIntValue(normal.x));
                MemoryUtil.memPutByte(o+this.vertexSize*1+normalOffset+1, normalIntValue(normal.y));
                MemoryUtil.memPutByte(o+this.vertexSize*1+normalOffset+2, normalIntValue(normal.z));
                MemoryUtil.memPutByte(o+this.vertexSize*2+normalOffset+0, normalIntValue(normal.x));
                MemoryUtil.memPutByte(o+this.vertexSize*2+normalOffset+1, normalIntValue(normal.y));
                MemoryUtil.memPutByte(o+this.vertexSize*2+normalOffset+2, normalIntValue(normal.z));
                MemoryUtil.memPutByte(o+this.vertexSize*3+normalOffset+0, normalIntValue(normal.x));
                MemoryUtil.memPutByte(o+this.vertexSize*3+normalOffset+1, normalIntValue(normal.y));
                MemoryUtil.memPutByte(o+this.vertexSize*3+normalOffset+2, normalIntValue(normal.z));
                this.elementsToFill &= ~VertexFormatElement.NORMAL.mask();
            }
            else {
                setNormal(0.0F, 1.0F, 0.0F);
            }
        }
        if ((elementsToFill & CanPipeVertexFormatElements.AO.mask()) != 0) {
            setAO(1.0F);
        }
        if ((elementsToFill & CanPipeVertexFormatElements.MATERIAL_INDEX.mask()) != 0) {
            setSharedMaterialIndex(-1);
            inheritMaterialIndex();
        }
        if ((elementsToFill & CanPipeVertexFormatElements.SPRITE_INDEX.mask()) != 0) {
            setSharedSpriteIndex(-1);
            inheritSpriteIndex();
        }
        if ((elementsToFill & CanPipeVertexFormatElements.TANGENT.mask()) != 0) {
            setSharedTangent(1.0F, 0.0F, 0.0F);
            inheritTangent();
        }
    }

    @Override
    public void setAO(float ao) {
        long offset = this.beginElement(CanPipeVertexFormatElements.AO);
        MemoryUtil.memPutFloat(offset, ao);
    }

    @Override
    public void setSharedSpriteIndex(int spriteIndex) {
        this.sharedSpriteIndex = spriteIndex;
    }

    @Override
    public void inheritSpriteIndex() {
        long offset = this.beginElement(CanPipeVertexFormatElements.SPRITE_INDEX);
        MemoryUtil.memPutInt(offset, this.sharedSpriteIndex);
    }

    @Override
    public void setSharedMaterialIndex(int materialIndex) {
        this.sharedMaterialIndex = materialIndex;
    }

    @Override
    public void inheritMaterialIndex() {
        long offset = this.beginElement(CanPipeVertexFormatElements.MATERIAL_INDEX);
        MemoryUtil.memPutInt(offset, this.sharedMaterialIndex);
    }

    @Override
    public void inheritTangent() {
        long offset = this.beginElement(CanPipeVertexFormatElements.TANGENT);
        MemoryUtil.memPutByte(offset+0, normalIntValue(this.sharedTangent.x));
        MemoryUtil.memPutByte(offset+1, normalIntValue(this.sharedTangent.y));
        MemoryUtil.memPutByte(offset+2, normalIntValue(this.sharedTangent.z));
        MemoryUtil.memPutByte(offset+3, (byte) 0);
    }

    @Override
    public void setSharedTangent(float x, float y, float z) {
        this.sharedTangent.set(x, y, z);
    }

}
