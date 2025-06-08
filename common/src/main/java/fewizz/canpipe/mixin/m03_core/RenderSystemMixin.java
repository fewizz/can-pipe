package fewizz.canpipe.mixin.m03_core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;

@Mixin(value = RenderSystem.class, priority = 1000, remap = false)
public class RenderSystemMixin {

    @SuppressWarnings("unused")
    private static GpuBuffer CANPIPE_QUAD_VERTEX_UV_BUFFER;

    @Shadow public static GpuDevice getDevice() { return null; }

    @Inject(method = "initRenderer", at = @At("RETURN"))
    private static void onInitRenderer(CallbackInfo ci) {
        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(DefaultVertexFormat.POSITION_COLOR.getVertexSize() * 4)) {
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferBuilder.addVertex(0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F);
            bufferBuilder.addVertex(1.0F, 0.0F, 0.0F).setUv(1.0F, 0.0F);
            bufferBuilder.addVertex(1.0F, 1.0F, 0.0F).setUv(1.0F, 1.0F);
            bufferBuilder.addVertex(0.0F, 1.0F, 0.0F).setUv(0.0F, 1.0F);

            try (MeshData meshData = bufferBuilder.buildOrThrow()) {
                CANPIPE_QUAD_VERTEX_UV_BUFFER = getDevice().createBuffer(() -> "Quad POSITION_TEX", GpuBuffer.USAGE_VERTEX, meshData.vertexBuffer());
            }
        }
    }

}
