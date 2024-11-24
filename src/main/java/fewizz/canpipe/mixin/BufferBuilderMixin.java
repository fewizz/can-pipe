package fewizz.canpipe.mixin;

import java.util.stream.Collectors;

import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

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
    protected static byte normalIntValue(float f){return 0;}

    /**
     * @reason no reason
     * @author fewizz
     * */
    @Overwrite
    private void endLastVertex() {
        if (this.vertices != 0) {
            if (this.elementsToFill != 0) {
                if ((elementsToFill & CanPipeVertexFormatElements.AO.mask()) != 0) {
                    setAO(1.0F);  // .
                }
                if ((elementsToFill & CanPipeVertexFormatElements.SPRITE_INDEX.mask()) != 0) {
                    setSpriteIndex(-1);  // .
                }
                if ((elementsToFill & CanPipeVertexFormatElements.TANGENT.mask()) != 0) {
                    setTangent(0);  // .
                }
                if (elementsToFill != 0) {  // .
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
    public void setSpriteIndex(int index) {
        long offset = this.beginElement(CanPipeVertexFormatElements.SPRITE_INDEX);
        MemoryUtil.memPutInt(offset, index);
    }

    @Override
    public void setTangent(int tangentPacked) {
        long offset = this.beginElement(CanPipeVertexFormatElements.TANGENT);
        MemoryUtil.memPutInt(offset, tangentPacked);
    }

}
