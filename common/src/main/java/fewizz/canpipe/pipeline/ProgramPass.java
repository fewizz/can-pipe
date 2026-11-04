package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.joml.Vector2i;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout.UniformDescription;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.UniformBufferStruct;
import fewizz.canpipe.Uniforms;
import fewizz.canpipe.UniformBufferStruct.IVec2Uniform;
import fewizz.canpipe.UniformBufferStruct.IntUniform;
import fewizz.canpipe.UniformBufferStruct.Mat4Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

public class ProgramPass extends Pass {

    private final Framebuffer framebuffer;
    private final FrontendRenderPipeline compiledRenderPipeline;
    // Textures (specified in "samplers": ["X", "Y"]) may not exist,
    // and that's ok if program doesn't actually use them
    private final List<AbstractTexture> textures;
    private final Vector2i extent;

    private final UniformBufferStruct pass = new UniformBufferStruct();
    private final IVec2Uniform frxSizeUniform = pass.add(new IVec2Uniform());
    private final IntUniform frxLodUniform = pass.add(new IntUniform());
    private final IntUniform frxLayerUniform = pass.add(new IntUniform());
    private final Mat4Uniform frxFrameProjectionMatrix = pass.add(new Mat4Uniform());
    private final GpuBuffer passUbo;

    public ProgramPass(
        Identifier id, Framebuffer framebuffer,
        CompiledRenderPipeline compiledRenderPipeline,
        List<Optional<AbstractTexture>> samplerTextures,
        Vector2i extent, int lod, int layer
    ) {
        super(id);

        this.textures = new ArrayList<>();
        this.compiledRenderPipeline = (FrontendRenderPipeline) compiledRenderPipeline;

        var samplers = this.compiledRenderPipeline.uniforms().stream()
            .filter(u -> u.type() == UniformType.COMBINED_IMAGE_SAMPLER)
            .toList();

        if (samplers.size() > samplerTextures.size()) {
            CanPipe.LOGGER.warn("Program \""+this.compiledRenderPipeline.name()+"\" has more samplers than textures provided by pass \""+id+"\"");
        }
        if (samplers.size() < samplerTextures.size()) {
            CanPipe.LOGGER.warn("Program \""+this.compiledRenderPipeline.name()+"\" has less samplers than textures provided by pass \""+id+"\"");
        }
        for (int i = 0; i < Math.min(samplers.size(), samplerTextures.size()); ++i) {
            String sampler = samplers.get(i).name();
            var samplerTexture = samplerTextures.get(i).orElseGet(() -> {
                CanPipe.LOGGER.warn("Couldn't find texture for sampler \""+sampler +"\", using \"white\" texture instead");
                return new AbstractTexture() {{
                    this.texture = CanPipe.getWhitePixelTexture();
                    this.textureView = CanPipe.getWhitePixelTextureView();
                }};
            });
            this.textures.add(samplerTexture);
        }

        this.passUbo = RenderSystem.getDevice().createBuffer(
            () -> "can-pipe \""+this.id+"\" pass UBO",
            GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
            pass.size()
        );

        this.framebuffer = framebuffer;
        this.extent = extent;

        this.frxLodUniform.set(lod);
        this.frxLayerUniform.set(layer);
        this.frxSizeUniform.set(-1);
    }

    @Override
    public void apply(CommandEncoder commandEncoder) {
        try {
            Minecraft mc = Minecraft.getInstance();

            int w = this.extent.x;
            int h = this.extent.y;

            if (w == 0) w = mc.gameRenderer.mainRenderTarget().width;
            if (h == 0) h = mc.gameRenderer.mainRenderTarget().height;

            w >>= this.frxLodUniform.get();
            h >>= this.frxLodUniform.get();

            var autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
            var indexBuffer = autoStorageIndexBuffer.getBuffer(6);
            var vertexBuffer = CanPipe.getQuadBuffer();

            if (this.frxSizeUniform.x != w || this.frxSizeUniform.y != h) {
                this.frxSizeUniform.set(w, h);
                this.frxFrameProjectionMatrix.setOrtho2D(0, w, 0, h);

                try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                    var builder = Std140Builder.onStack(memoryStack, this.pass.size());
                    this.pass.writeTo(builder);
                    commandEncoder.writeToBuffer(this.passUbo.slice(), builder.get());
                }
            }

            var descriptorBuilder = RenderPassDescriptor.builder(() -> "Program pass "+this.id);
            for (var colorAttachment : this.framebuffer.colorTextureViews) { descriptorBuilder.withColorAttachment(colorAttachment); }
            if (this.framebuffer.getDepthTextureView() != null) { descriptorBuilder.withDepthAttachment(this.framebuffer.getDepthTextureView()); }
            descriptorBuilder.withRenderArea(new RenderPass.RenderArea(0, 0, w, h));

            try (RenderPass renderPass = commandEncoder.createRenderPass(descriptorBuilder.build())) {
                renderPass.setPipeline(this.compiledRenderPipeline);

                List<UniformDescription> samplers = this.compiledRenderPipeline.uniforms().stream()
                    .filter(u -> u.type() == UniformType.COMBINED_IMAGE_SAMPLER)
                    .toList();

                for (int i = 0; i < Math.min(samplers.size(), this.textures.size()); ++i) {
                    String sampler = samplers.get(i).name();
                    var samplerTexture = this.textures.get(i);
                    renderPass.setUniform(sampler, samplerTexture.getTextureView(), this.textures.get(i).getSampler());
                }

                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("DynamicTransforms", Uniforms.DYNAMIC_TRANSFORMS_UBO);
                renderPass.setUniform("canpipe_ub_pass", this.passUbo);
                Uniforms.setRenderPassFREXUniforms(renderPass);

                renderPass.setVertexBuffer(0, vertexBuffer.slice());
                renderPass.setIndexBuffer(indexBuffer, autoStorageIndexBuffer.type());
                renderPass.drawIndexed(6, 1, 0, 0, 0);
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't apply program pass \""+this.id+"\" with render pipeline \""+this.compiledRenderPipeline.name()+"\"", e);
        }
    }

    @Override
    public void close() {
        this.passUbo.close();
    };

}
