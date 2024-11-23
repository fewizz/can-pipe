package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;

import fewizz.canpipe.CanPipeRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;

@Mixin(SectionBufferBuilderPack.class)
public class SectionBufferBuilderPackMixin {

    @WrapMethod(method = "buffer")
    ByteBufferBuilder replaceBuffer(RenderType renderType, Operation<ByteBufferBuilder> original) {
        return original.call(CanPipeRenderTypes.unreplaced(renderType));
    }

}
