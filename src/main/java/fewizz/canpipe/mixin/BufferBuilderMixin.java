package fewizz.canpipe.mixin;

import java.util.stream.Collectors;

import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import fewizz.canpipe.CanPipeVertexFormatElements;
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
    abstract protected long beginElement(VertexFormatElement vertexFormatElement);

    @Shadow
    protected static byte normalIntValue(float f) {return 0;}

    @Unique
    final Vector3f sharedTangent = new Vector3f(1.0F, 0.0F, 0.0F);

    @Unique
    int sharedMaterialIndex = -1;

    @Unique
    int sharedSpriteIndex = -1;

    /**
     * @reason laziness, TODO
     * @author fewizz
     * */
    @Overwrite
    private void endLastVertex() {
        if (this.vertices != 0) {
            if (this.elementsToFill != 0) {
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
                if (elementsToFill != 0) {
                    String string = (String)VertexFormatElement.elementsFromMask(this.elementsToFill).map(this.format::getElementName).collect(Collectors.joining(", "));
                    throw new IllegalStateException("Missing elements in vertex: " + string);
                }
            } else {
                if (this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP) {
                    long l = this.buffer.reserve(this.vertexSize);
                    MemoryUtil.memCopy(l - (long)this.vertexSize, l, (long)this.vertexSize);
                    this.vertices++;
                }
            }
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
