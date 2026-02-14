package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;

@Mixin(ByteBufferBuilder.class)
public interface ByteBufferBuilderAccessor {

    @Accessor("pointer")
    long canpipe_getPointer();

}
