package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;

@Mixin(value = RenderSystem.class, priority = 1001, remap = false)
public interface RenderSystemAccessor {

    @Accessor(value = "CANPIPE_QUAD_VERTEX_UV_BUFFER")
    public static GpuBuffer canpipe_getQuadBuffer() { return null; }

}
