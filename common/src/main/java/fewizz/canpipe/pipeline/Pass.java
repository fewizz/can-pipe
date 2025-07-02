package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.mixin.RenderSystemAccessor;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import net.minecraft.client.Minecraft;

public class Pass extends PassBase {

    final Framebuffer framebuffer;
    final RenderPipeline renderPipeline;
    // Textures (specified in "samplers": ["X", "Y"]) may not exist,
    // and that's ok if program doesn't actually uses them
    final List<Optional<? extends GpuTextureView>> textureViews;
    final Vector2i extent;
    final int lod;
    final int layer;

    private Pass(
        String name, Framebuffer framebuffer, RenderPipeline renderPipeline,
        List<Optional<? extends GpuTextureView>> textureViews,
        Vector2i extent, int lod, int layer
    ) {
        super(name);
        var samplers = renderPipeline.getSamplers();
        if (samplers.size() > textureViews.size()) {
            CanPipe.LOGGER.warn("Program \""+renderPipeline.getLocation()+"\" has more samplers than textures provided by pass \""+name+"\"");
        }
        if (samplers.size() < textureViews.size()) {
            CanPipe.LOGGER.warn("Program \""+renderPipeline.getLocation()+"\" has less samplers than textures provided by pass \""+name+"\"");
        }
        for (int i = 0; i < Math.min(samplers.size(), textureViews.size()); ++i) {
            String sampler = samplers.get(i);
            Optional<? extends GpuTextureView> texture = textureViews.get(i);
            if (texture.isEmpty() && renderPipeline.getSamplers() != null) {
                throw new NullPointerException("Couldn't find texture for sampler \""+sampler +"\"");
            }
        }

        this.framebuffer = framebuffer;
        this.renderPipeline = renderPipeline;
        this.textureViews = textureViews;
        this.extent = extent;
        this.lod = lod;
        this.layer = layer;
    }

    @Override
    public void apply() {
        Minecraft mc = Minecraft.getInstance();

        int w = this.extent.x;
        int h = this.extent.y;

        if (w == 0) w = mc.getMainRenderTarget().width;
        if (h == 0) h = mc.getMainRenderTarget().height;

        w >>= this.lod;
        h >>= this.lod;

        var autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(Mode.QUADS);
        var indexBuffer = autoStorageIndexBuffer.getBuffer(6);
        var vertexBuffer = RenderSystemAccessor.canpipe_getQuadBuffer();

        Programs.FRX_SIZE.set((int) w, (int) h);
        Programs.FRX_LOD.set(lod);
        Programs.FRX_LAYER.set(layer);
        Programs.FRX_FRAME_PROJECTION_MATRIX.setOrtho2D(0, w, 0, h);

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            var builder = Std140Builder.onStack(memoryStack, Programs.PASS.size());
            Programs.PASS.writeTo(builder);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(Programs.PASS_UBO.slice(), builder.get());
        }

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
            ((GameRendererExtended)mc.gameRenderer).canpipe_worldViewMatrix(),
            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f(), 0.0F
        );

        try (
            RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                    () -> "can-pipe pass \""+this.name+"\"",
                    this.framebuffer.getColorTextureView(), OptionalInt.empty()
                )
        ) {
            renderPass.setPipeline(this.renderPipeline);

            var samplers = this.renderPipeline.getSamplers();
            for (int i = 0; i < Math.min(samplers.size(), this.textureViews.size()); ++i) {
                String sampler = samplers.get(i);
                this.textureViews.get(i).ifPresent(texture -> {
                    renderPass.bindSampler(sampler, texture);
                });
            }

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);

            renderPass.setUniform("canpipe_ub_pass", Programs.PASS_UBO);
            renderPass.setVertexBuffer(0, vertexBuffer);
            renderPass.setIndexBuffer(indexBuffer, autoStorageIndexBuffer.type());
            renderPass.drawIndexed(0, 0, 6, 0);
        }
    }

    static Optional<PassBase> load(
        JsonObject json,
        Function<String, Object> optionValueByName,
        Function<String, Optional<Framebuffer>> getOrLoadOptionalFramebuffer,
        Function<String, RenderPipeline> getOrLoadProgram,
        Function<String, Optional<GpuTextureView>> getOrLoadPipelineOrResourcepackTextureView
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

        RenderPipeline program = getOrLoadProgram.apply(programName);
        Objects.nonNull(program);

        List<Optional<? extends GpuTextureView>> textureViews = new ArrayList<>();
        for (String s : JanksonUtils.listOfStrings(json, "samplerImages")) {
            textureViews.add(getOrLoadPipelineOrResourcepackTextureView.apply(s));
        }

        int size = json.getInt("size", 0);
        Vector2i extent = new Vector2i(
            json.getInt("width", size),
            json.getInt("height", size)
        );
        int lod = json.getInt("lod", 0);
        int layer = json.getInt("layer", 0);

        return Optional.of(new Pass(passName, framebuffer.get(), program, textureViews, extent, lod, layer));
    }

    static class FREXClear extends PassBase {
        final Framebuffer framebuffer;

        FREXClear(String name, Framebuffer framebuffer) {
            super(name);
            this.framebuffer = framebuffer;
        }

        @Override
        public void apply() {
            framebuffer.bindAndClearFully();
        }

    };

}
