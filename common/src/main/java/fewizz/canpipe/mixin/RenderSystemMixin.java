package fewizz.canpipe.mixin;

import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.CanPipe;

@Mixin(value = RenderSystem.class, priority = 1000)
public class RenderSystemMixin {

    @SuppressWarnings("unused") private static GpuBuffer CANPIPE_QUAD_VERTEX_UV_BUFFER;
    @SuppressWarnings("unused") private static GpuBuffer[] CANPIPE_INT_0_3_UBO_BUFFERS;
    @SuppressWarnings("unused") private static GpuTexture CANPIPE_WHITE_TEXTURE;
    @SuppressWarnings("unused") private static GpuTextureView CANPIPE_WHITE_TEXTURE_VIEW;

    @Shadow public static GpuDevice getDevice() { return null; }

    @Inject(method = "initRenderer", at = @At("RETURN"))
    private static void onInitRenderer(CallbackInfo ci) {
        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(CanPipe.VertexFormats.POSITION_TEX.getVertexSize() * 4)) {
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS, CanPipe.VertexFormats.POSITION_TEX);
            bufferBuilder.addVertex(0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F);
            bufferBuilder.addVertex(1.0F, 0.0F, 0.0F).setUv(1.0F, 0.0F);
            bufferBuilder.addVertex(1.0F, 1.0F, 0.0F).setUv(1.0F, 1.0F);
            bufferBuilder.addVertex(0.0F, 1.0F, 0.0F).setUv(0.0F, 1.0F);

            try (MeshData meshData = bufferBuilder.buildOrThrow()) {
                CANPIPE_QUAD_VERTEX_UV_BUFFER = getDevice().createBuffer(
                    () -> "can-pipe quad",
                    GpuBuffer.USAGE_VERTEX,
                    meshData.vertexBuffer()
                );
            }
        }

        CANPIPE_INT_0_3_UBO_BUFFERS = new GpuBuffer[] {
            RenderSystem.getDevice().createBuffer(
                () -> "can-pipe 0",
                GpuBuffer.USAGE_UNIFORM,
                MemoryUtil.memByteBuffer(MemoryUtil.memAllocInt(1).put(0, 0))
            ),
            RenderSystem.getDevice().createBuffer(
                () -> "can-pipe 1",
                GpuBuffer.USAGE_UNIFORM,
                MemoryUtil.memByteBuffer(MemoryUtil.memAllocInt(1).put(0, 1))
            ),
            RenderSystem.getDevice().createBuffer(
                () -> "can-pipe 2",
                GpuBuffer.USAGE_UNIFORM,
                MemoryUtil.memByteBuffer(MemoryUtil.memAllocInt(1).put(0, 2))
            ),
            RenderSystem.getDevice().createBuffer(
                () -> "can-pipe 3",
                GpuBuffer.USAGE_UNIFORM,
                MemoryUtil.memByteBuffer(MemoryUtil.memAllocInt(1).put(0, 3))
            )
        };

        CANPIPE_WHITE_TEXTURE = RenderSystem.getDevice().createTexture(
            "can-pipe white", GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST, TextureFormat.RGBA8,
            1, 1, 1, 1
        );
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(CANPIPE_WHITE_TEXTURE, 0xFFFFFFFF);

        CANPIPE_WHITE_TEXTURE_VIEW = RenderSystem.getDevice().createTextureView(CANPIPE_WHITE_TEXTURE);
    }

}
