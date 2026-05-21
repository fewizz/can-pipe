package fewizz.canpipe.mixin;

import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

@Mixin(value = RenderSystem.class, priority = 1001)
public interface RenderSystemAccessor {

    @Accessor(value = "CANPIPE_QUAD_VERTEX_UV_BUFFER")
    static @NonNull GpuBuffer canpipe_getQuadBuffer() { return null; }

    @Accessor(value = "CANPIPE_INT_0_3_UBO_BUFFERS")
    static @NonNull GpuBuffer[] canpipe_get0to3UBOBuffers() { return null; }

    @Accessor(value = "CANPIPE_WHITE_TEXTURE")
    static @NonNull GpuTexture canpipe_getWhiteTexture() { return null; }

    @Accessor(value = "CANPIPE_WHITE_TEXTURE_VIEW")
    static @NonNull GpuTextureView canpipe_getWhiteTextureView() { return null; }

}
