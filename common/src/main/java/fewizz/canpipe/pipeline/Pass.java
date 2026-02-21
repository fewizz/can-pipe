package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.joml.Vector2i;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.UniformBufferStruct;
import fewizz.canpipe.UniformBufferStruct.IVec2Uniform;
import fewizz.canpipe.UniformBufferStruct.IntUniform;
import fewizz.canpipe.UniformBufferStruct.Mat4Uniform;
import fewizz.canpipe.Uniforms;
import fewizz.canpipe.b3d.CommandEncoderBackendExtended;
import fewizz.canpipe.b3d.GpuTextureViewExtended;
import fewizz.canpipe.mixin.RenderSystemAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

public class Pass extends PassBase {

    final Framebuffer framebuffer;
    final RenderPipeline renderPipeline;
    // Textures (specified in "samplers": ["X", "Y"]) may not exist,
    // and that's ok if program doesn't actually uses them
    final List<AbstractTexture> textures;
    final Vector2i extent;

    final UniformBufferStruct pass = new UniformBufferStruct();
    final IVec2Uniform frx_size = pass.add(new IVec2Uniform());
    final IntUniform frx_lod = pass.add(new IntUniform());
    final IntUniform frx_layer = pass.add(new IntUniform());
    final Mat4Uniform frx_frame_projection_matrix = pass.add(new Mat4Uniform());
    private GpuBuffer passUbo = null;

    private Pass(
        String name, Framebuffer framebuffer, RenderPipeline renderPipeline,
        List<Optional<AbstractTexture>> samplerTextures,
        Vector2i extent, int lod, int layer
    ) {
        super(name);
        this.textures = new ArrayList<>();

        var samplers = renderPipeline.getSamplers();
        if (samplers.size() > samplerTextures.size()) {
            CanPipe.LOGGER.warn("Program \""+renderPipeline.getLocation()+"\" has more samplers than textures provided by pass \""+name+"\"");
        }
        if (samplers.size() < samplerTextures.size()) {
            CanPipe.LOGGER.warn("Program \""+renderPipeline.getLocation()+"\" has less samplers than textures provided by pass \""+name+"\"");
        }
        for (int i = 0; i < Math.min(samplers.size(), samplerTextures.size()); ++i) {
            String sampler = samplers.get(i);
            var samplerTexture = samplerTextures.get(i).orElseGet(() -> {
                CanPipe.LOGGER.warn("Couldn't find texture for sampler \""+sampler +"\", \"barrier\" texture will be used instead");
                Minecraft mc = Minecraft.getInstance();
                return mc.getTextureManager().getTexture(
                    Identifier.withDefaultNamespace("textures/item/barrier.png")
                );
            });
            this.textures.add(samplerTexture);
        }

        this.framebuffer = framebuffer;
        this.renderPipeline = renderPipeline;
        this.extent = extent;

        this.frx_lod.set(lod);
        this.frx_layer.set(layer);
        this.frx_size.set(-1);
    }

    @Override
    public void apply(CommandEncoderBackendExtended commandEncoder) {
        Minecraft mc = Minecraft.getInstance();

        int w = this.extent.x;
        int h = this.extent.y;

        if (w == 0) w = mc.getMainRenderTarget().width;
        if (h == 0) h = mc.getMainRenderTarget().height;

        w >>= this.frx_lod.get();
        h >>= this.frx_lod.get();

        var autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(Mode.QUADS);
        var indexBuffer = autoStorageIndexBuffer.getBuffer(6);
        var vertexBuffer = RenderSystemAccessor.canpipe_getQuadBuffer();

        if (this.frx_size.x != w || this.frx_size.y != h) {
            this.frx_size.set(w, h);
            this.frx_frame_projection_matrix.setOrtho2D(0, w, 0, h);

            if (this.passUbo != null) { this.passUbo.close(); }

            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                var builder = Std140Builder.onStack(memoryStack, this.pass.size());
                this.pass.writeTo(builder);
                this.passUbo = RenderSystem.getDevice().createBuffer(
                    () -> "can-pipe \""+this.name+"\" pass UBO", GpuBuffer.USAGE_UNIFORM, builder.get()
                );
            }
        }

        try (
            RenderPassBackend renderPass = commandEncoder.canpipe_createRenderPass(
                () -> "can-pipe pass \""+this.name+"\"",
                this.framebuffer.colorTextureViews,
                this.framebuffer.getDepthTextureView()
            )
        ) {
            renderPass.setPipeline(this.renderPipeline);

            var samplers = this.renderPipeline.getSamplers();
            for (int i = 0; i < Math.min(samplers.size(), this.textures.size()); ++i) {
                String sampler = samplers.get(i);
                var samplerTexture = this.textures.get(i);
                renderPass.bindTexture(sampler, samplerTexture.getTextureView(), this.textures.get(i).getSampler());
            }

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", Uniforms.PASS_DYNAMIC_TRANSFORMS_UBO);
            renderPass.setUniform("canpipe_ub_pass", this.passUbo);
            renderPass.setUniform("frx_ub_accessibility", Uniforms.ACCESSIBILITY_UBO);
            renderPass.setUniform("frx_ub_view", Uniforms.VIEW_UBO);
            renderPass.setUniform("frx_ub_shadow", Uniforms.SHADOW_UBO);
            renderPass.setUniform("frx_ub_player", Uniforms.PLAYER_UBO);
            renderPass.setUniform("frx_ub_world", Uniforms.WORLD_UBO);
            renderPass.setUniform("frx_ub_fog", Uniforms.FOG_UBO);

            renderPass.setVertexBuffer(0, vertexBuffer);
            renderPass.setIndexBuffer(indexBuffer, autoStorageIndexBuffer.type());
            renderPass.drawIndexed(0, 0, 6, 1);
        }
    }

    @Override
    public void close() {
        if (this.passUbo != null) {
            this.passUbo.close();
        }
    };

    static Optional<PassBase> load(
        JsonObject json,
        Function<String, Object> optionValueByName,
        Function<String, Optional<Framebuffer>> getOrLoadOptionalFramebuffer,
        Function<String, RenderPipeline> getOrLoadProgram,
        Function<String, Optional<AbstractTexture>> getOrLoadPipelineOrResourcepackTexture
    ) {
        String toggleConfig = json.get(String.class, "toggleConfig");

        // pass is disabled, skipping
        if (toggleConfig != null && !(boolean) optionValueByName.apply(toggleConfig)) {
            return Optional.empty();
        }

        String passName = json.get(String.class, "name");
        String framebufferName = json.get(String.class, "framebuffer");
        Optional<Framebuffer> framebuffer = getOrLoadOptionalFramebuffer.apply(framebufferName);
        if (framebuffer.isEmpty()) {
            // canvas behaviour
            CanPipe.LOGGER.warn("Couldn't find framebuffer \""+framebufferName +"\", pass \""+passName+"\" will be skipped");
            return Optional.empty();
        }

        String programName = json.get(String.class, "program");

        if (programName.equals("frex_clear")) {
            return Optional.of(new Pass.FREXClear(passName, framebuffer.get()));
        }

        RenderPipeline renderPipeline = getOrLoadProgram.apply(programName);
        Objects.nonNull(renderPipeline);

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

        return Optional.of(new Pass(passName, framebuffer.get(), renderPipeline, samplerTextures, extent, lod, layer));
    }

    static class FREXClear extends PassBase {
        final Framebuffer framebuffer;

        FREXClear(String name, Framebuffer framebuffer) {
            super(name);
            this.framebuffer = framebuffer;
        }

        @Override
        public void apply(CommandEncoderBackendExtended commandEncoder) {
            for (int i = 0; i < this.framebuffer.colorTextureViews.length; ++i) {
                commandEncoder.canpipe_clearColorTexture(
                    this.framebuffer.colorTextures[i],
                    this.framebuffer.colorTextureClearColors[i],
                    this.framebuffer.colorTextureViews[i].baseMipLevel(),
                    this.framebuffer.colorTextureViews[i].mipLevels(),
                    ((GpuTextureViewExtended) this.framebuffer.colorTextureViews[i]).canpipe_baseArrayLayer(),
                    ((GpuTextureViewExtended) this.framebuffer.colorTextureViews[i]).canpipe_layerCount()
                );
            }
            if (this.framebuffer.getDepthTexture() != null) {
                commandEncoder.canpipe_clearDepthTexture(
                    this.framebuffer.getDepthTexture(),
                    this.framebuffer.depthTextureClearDepth,
                    this.framebuffer.getDepthTextureView().baseMipLevel(),
                    this.framebuffer.getDepthTextureView().mipLevels(),
                    ((GpuTextureViewExtended) this.framebuffer.getDepthTextureView()).canpipe_baseArrayLayer(),
                    ((GpuTextureViewExtended) this.framebuffer.getDepthTextureView()).canpipe_layerCount()
                );
            }
        }

        @Override
        public void close() {}

    }

}
