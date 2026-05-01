package fewizz.canpipe.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;

import fewizz.canpipe.mixin.ByteBufferBuilderAccessor;
import fewizz.canpipe.mixin.RenderSetupAccessor;
import fewizz.canpipe.mixin.RenderTypeAccessor;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

public class PerVertexFormatBufferSource extends MultiBufferSource.BufferSource {

    record SliceInfo(RenderType renderType, int vertexFirst, int indexCount) {}

    private static class VertexFormatBufferSource {
        private RenderType lastRenderType = null;
        private BufferBuilder lastBufferBuilder = null;

        private final ByteBufferBuilder vertexByteBufferBuilder;

        private final List<SliceInfo> vertexBufferSlices = new ArrayList<>();

        private int vertices = 0;

        private VertexFormatBufferSource() {
            this.vertexByteBufferBuilder = new ByteBufferBuilder(768 * 1024);
        }

        private BufferBuilder getBuffer(RenderType renderType) {
            if (this.lastRenderType != renderType) {
                this.endLastBufferBuilder();
                this.lastBufferBuilder = new BufferBuilder(this.vertexByteBufferBuilder, renderType.mode(), renderType.format());
            }

            this.lastRenderType = renderType;
            return this.lastBufferBuilder;
        }

        private void endLastBufferBuilder() {
            if (this.lastBufferBuilder != null) {
                var mesh = this.lastBufferBuilder.build();

                if (mesh != null) {
                    int vertexFirst = this.vertices;
                    int vertexCount = mesh.drawState().vertexCount();
                    int indexCount = mesh.drawState().indexCount();

                    this.vertices += vertexCount;
                    this.vertexBufferSlices.add(new SliceInfo(this.lastRenderType, vertexFirst, indexCount));
                }

                this.vertexByteBufferBuilder.build();  // increase `resultCount` field
            }
        }

        private void reset() {
            this.vertexByteBufferBuilder.discard();
            this.vertexBufferSlices.clear();
            this.lastRenderType = null;
            this.lastBufferBuilder = null;
            this.vertices = 0;
        }
    }

    Map<VertexFormat, VertexFormatBufferSource> bufferSources = new HashMap<>();

    public PerVertexFormatBufferSource() {
        super(null, Collections.emptyNavigableMap());
    }

    @Override
    public @NonNull VertexConsumer getBuffer(RenderType renderType) {
        var bufferSource = this.bufferSources.computeIfAbsent(renderType.format(), k -> new VertexFormatBufferSource());
        return bufferSource.getBuffer(renderType);
    }

    @Override
    public void endLastBatch() {}

    @Override
    public void endBatch(@NonNull RenderType renderType) {}

    @Override
    public void endBatch() {
        Pipeline pipeline = Pipelines.getCurrent();
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f()
        );

        for (var bufferSource : this.bufferSources.values()) {
            bufferSource.endLastBufferBuilder();

            if (bufferSource.vertexBufferSlices.isEmpty()) {
                continue;
            }

            var lastElement = bufferSource.vertexBufferSlices.getLast();

            var begin = ((ByteBufferBuilderAccessor) bufferSource.vertexByteBufferBuilder).canpipe_getPointer();
            int capacity = (int) Math.subtractExact(bufferSource.vertexByteBufferBuilder.reserve(0), begin);
            GpuBuffer vertexBuffer = lastElement.renderType.format().uploadImmediateVertexBuffer(MemoryUtil.memByteBuffer(begin, capacity));

            RenderPass renderPass = null;
            RenderTarget renderTarget = null;
            RenderPipeline renderPipeline = null;

            try {
                for (var s : bufferSource.vertexBufferSlices) {
                    RenderSetup renderSetup = ((RenderTypeAccessor) s.renderType).canpipe_getState();
                    renderSetup.getTextures();  // preload textures before creating renderpass
                }

                for (var s : bufferSource.vertexBufferSlices) {
                    RenderSetup renderSetup = ((RenderTypeAccessor) s.renderType).canpipe_getState();
                    RenderTarget newRenderTarget = ((RenderSetupAccessor) (Object) renderSetup).canpipe_getOutputTarget().getRenderTarget();

                    newRenderTarget = pipeline.replaceRenderTarget(newRenderTarget, renderSetup);

                    if (renderTarget != newRenderTarget) {
                        if (renderPass != null) {
                            renderPass.close();
                        }
                        renderTarget = newRenderTarget;
                        renderPass = pipeline.createRenderPass(
                            commandEncoder,
                            () -> "can-pipe immediate for \""+((RenderTypeAccessor) s.renderType).canpipe_getName()+"\"-like rendertype",
                            (Framebuffer) renderTarget
                        );
                    }

                    if (renderPipeline != s.renderType.pipeline()) {
                        renderPass.setPipeline(s.renderType.pipeline());

                        ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();
                        if (scissorState.enabled()) {
                            renderPass.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height());
                        }

                        RenderSystem.bindDefaultUniforms(renderPass);
                        renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                        renderPipeline = s.renderType.pipeline();
                    }

                    var textures = renderSetup.getTextures();
                    for (Entry<String, RenderSetup.TextureAndSampler> entry : textures.entrySet()) {
                        renderPass.bindTexture(
                            (String) entry.getKey(),
                            ((RenderSetup.TextureAndSampler) entry.getValue()).textureView(),
                            ((RenderSetup.TextureAndSampler) entry.getValue()).sampler()
                        );
                        if (entry.getKey().equals("Sampler0")) {
                            Pipeline.bindSpritesExtentsSampler(renderPass, entry.getValue().textureView());
                        }
                    }

                    renderPass.setVertexBuffer(0, vertexBuffer);

                    RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(s.renderType.mode());
                    GpuBuffer indexBuffer = autoStorageIndexBuffer.getBuffer(s.indexCount);

                    IndexType indexType = autoStorageIndexBuffer.type();
                    renderPass.setIndexBuffer(indexBuffer, indexType);

                    renderPass.drawIndexed(s.vertexFirst, 0, s.indexCount, 1);
                }
            } finally {
                if (renderPass != null) {
                    renderPass.close();
                }
            }
        }

        for (var vertexFormatBufferSource : this.bufferSources.values()) {
            vertexFormatBufferSource.reset();
        }
    }

}
