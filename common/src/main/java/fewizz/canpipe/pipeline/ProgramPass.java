package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.joml.Vector2i;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPass.RenderArea;
import com.mojang.blaze3d.systems.RenderPassDescriptor;
import com.mojang.blaze3d.systems.RenderSystem;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.pipeline.UniformBufferStruct.IVec2Uniform;
import fewizz.canpipe.pipeline.UniformBufferStruct.IntUniform;
import fewizz.canpipe.pipeline.UniformBufferStruct.Mat4Uniform;
import fewizz.canpipe.util.JanksonUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

public class ProgramPass extends Pass {

    private final Framebuffer framebuffer;
    private final RenderPipeline renderPipeline;
    // Textures (specified in "samplers": ["X", "Y"]) may not exist,
    // and that's ok if program doesn't actually use them
    private final List<AbstractTexture> textures;
    private final Vector2i extent;

    private final UniformBufferStruct pass = new UniformBufferStruct();
    private final IVec2Uniform frxSizeUniform = pass.add(new IVec2Uniform());
    private final IntUniform frxLoadUniform = pass.add(new IntUniform());
    private final IntUniform frxLayerUniform = pass.add(new IntUniform());
    private final Mat4Uniform frxFrameProjectionMatrix = pass.add(new Mat4Uniform());
    private final GpuBuffer passUbo;

    private ProgramPass(
        Identifier id, Framebuffer framebuffer, RenderPipeline renderPipeline,
        List<Optional<AbstractTexture>> samplerTextures,
        Vector2i extent, int lod, int layer
    ) {
        super(id);
        this.textures = new ArrayList<>();

        var samplers = renderPipeline.getBindGroupLayouts().get(0).getSamplers();
        if (samplers.size() > samplerTextures.size()) {
            CanPipe.LOGGER.warn("Program \""+renderPipeline.getLocation()+"\" has more samplers than textures provided by pass \""+id+"\"");
        }
        if (samplers.size() < samplerTextures.size()) {
            CanPipe.LOGGER.warn("Program \""+renderPipeline.getLocation()+"\" has less samplers than textures provided by pass \""+id+"\"");
        }
        for (int i = 0; i < Math.min(samplers.size(), samplerTextures.size()); ++i) {
            String sampler = samplers.get(i);
            var samplerTexture = samplerTextures.get(i).orElseGet(() -> {
                CanPipe.LOGGER.warn("Couldn't find texture for sampler \""+sampler +"\", using \"white\" texture instead");
                return new AbstractTexture() {{
                    this.texture = CanPipe.getWhiteTexture();
                    this.textureView = CanPipe.getWhiteTextureView();
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
        this.renderPipeline = renderPipeline;
        this.extent = extent;

        this.frxLoadUniform.set(lod);
        this.frxLayerUniform.set(layer);
        this.frxSizeUniform.set(-1);

        // Prelink
        RenderSystem.getDevice().precompilePipeline(renderPipeline);
    }

    @Override
    public void apply(CommandEncoder commandEncoder) {
        try {
            Minecraft mc = Minecraft.getInstance();

            int w = this.extent.x;
            int h = this.extent.y;

            if (w == 0) w = mc.gameRenderer.mainRenderTarget().width;
            if (h == 0) h = mc.gameRenderer.mainRenderTarget().height;

            w >>= this.frxLoadUniform.get();
            h >>= this.frxLoadUniform.get();

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

            RenderPassDescriptor renderPassDescriptor = RenderPassDescriptor.create(() -> "Program pass "+this.id);
            for (var colorAttachment : this.framebuffer.colorTextureViews) {
                renderPassDescriptor.withColorAttachment(colorAttachment);
            }
            if (this.framebuffer.getDepthTextureView() != null) {
                renderPassDescriptor.withDepthAttachment(this.framebuffer.getDepthTextureView());
            }
            renderPassDescriptor.withRenderArea(new RenderArea(0, 0, w, h));

            try (
                RenderPass renderPass = commandEncoder.createRenderPass(renderPassDescriptor)
            ) {
                renderPass.setPipeline(this.renderPipeline);

                var samplers = this.renderPipeline.getBindGroupLayouts().get(0).getSamplers();
                for (int i = 0; i < Math.min(samplers.size(), this.textures.size()); ++i) {
                    String sampler = samplers.get(i);
                    var samplerTexture = this.textures.get(i);
                    renderPass.bindTexture(sampler, samplerTexture.getTextureView(), this.textures.get(i).getSampler());
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
            throw new RuntimeException("Couldn't apply program pass \""+this.id+"\" with render pipeline \""+this.renderPipeline+"\"", e);
        }
    }

    @Override
    public void close() {
        this.passUbo.close();
    };

    static Optional<Pass> load(
        Identifier pipelineId,
        JsonObject json,
        Function<String, Object> optionValueByName,
        Function<String, Optional<Framebuffer>> getOrLoadOptionalFramebuffer,
        BiFunction<String, Framebuffer, RenderPipeline> getOrLoadProgram,
        Function<String, Optional<AbstractTexture>> getOrLoadPipelineOrResourcepackTexture
    ) {
        String toggleConfig = json.get(String.class, "toggleConfig");

        // pass is disabled, skipping
        if (toggleConfig != null && !(boolean) optionValueByName.apply(toggleConfig)) {
            return Optional.empty();
        }

        String passName = json.get(String.class, "name");
        Objects.requireNonNull(passName);
        String framebufferName = json.get(String.class, "framebuffer");
        Optional<Framebuffer> framebuffer = getOrLoadOptionalFramebuffer.apply(framebufferName);
        if (framebuffer.isEmpty()) {
            // canvas behaviour
            CanPipe.LOGGER.warn("Couldn't find framebuffer \""+framebufferName +"\", pass \""+passName+"\" will be skipped");
            return Optional.empty();
        }

        String programName = json.get(String.class, "program");
        Objects.requireNonNull(programName);
        var id = Identifier.fromNamespaceAndPath(pipelineId.getNamespace(), passName);

        if (programName.equals("frex_clear")) {
            return Optional.of(new ClearPass(id, framebuffer.get()));
        }

        RenderPipeline renderPipeline = getOrLoadProgram.apply(programName, framebuffer.get());
        Objects.requireNonNull(renderPipeline);

        List<Optional<AbstractTexture>> samplerTextures = new ArrayList<>();
        for (String s : JanksonUtils.listOfStrings(json, "samplerImages")) {
            samplerTextures.add(getOrLoadPipelineOrResourcepackTexture.apply(s));
        }

        int size = json.getInt("size", 0);
        Vector2i extent = new Vector2i(
            json.getInt("width", size),
            json.getInt("height", size)
        );
        int lod = json.getInt("lod", 0);
        int layer = json.getInt("layer", 0);

        return Optional.of(new ProgramPass(id, framebuffer.get(), renderPipeline, samplerTextures, extent, lod, layer));
    }

}
