package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import org.joml.Matrix4f;
import org.joml.Vector2i;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.mixin.m04_core.RenderSystemAccessor;
import net.minecraft.client.Minecraft;

public class Pass extends PassBase {

    final String name;
    final Framebuffer framebuffer;
    final Program program;
    // Textures (spcified in "samplers": ["X", "Y"]) may not exist,
    // and that's ok if program doesn't actually uses them
    final List<Optional<? extends GlTexture>> textures;
    final Vector2i extent;
    final int lod;
    final int layer;

    private Pass(
        String name, Framebuffer framebuffer, Program program,
        List<Optional<? extends GlTexture>> textures,
        Vector2i extent, int lod, int layer
    ) {
        var samplers = program.renderPipeline.getSamplers();
        if (samplers.size() > textures.size()) {
            CanPipe.LOGGER.warn("Program \""+program.getDebugLabel()+"\" has more samplers than textures provided by pass \""+name+"\"");
        }
        if (samplers.size() < textures.size()) {
            CanPipe.LOGGER.warn("Program \""+program.getDebugLabel()+"\" has less samplers than textures provided by pass \""+name+"\"");
        }
        for (int i = 0; i < Math.min(samplers.size(), textures.size()); ++i) {
            String sampler = samplers.get(i);
            Optional<? extends GlTexture> texture = textures.get(i);
            if (texture.isEmpty() && program.samplerExists(sampler)) {
                throw new NullPointerException("Couldn't find texture for sampler \""+sampler +"\"");
            }
        }

        this.name = name;
        this.framebuffer = framebuffer;
        this.program = program;
        this.textures = textures;
        this.extent = extent;
        this.lod = lod;
        this.layer = layer;
    }

    @Override
    public void apply(Matrix4f view, Matrix4f projection) {
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

        try (
            RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(this.framebuffer.getColorTexture(), OptionalInt.empty())
        ) {
            renderPass.setPipeline(this.program.renderPipeline);

            var samplers = this.program.renderPipeline.getSamplers();
            for (int i = 0; i < Math.min(samplers.size(), this.textures.size()); ++i) {
                String sampler = samplers.get(i);
                this.textures.get(i).ifPresent(texture -> {
                    renderPass.bindSampler(sampler, texture);
                });
            }

            renderPass.setUniform("frxu_size", (int) w, (int) h);
            renderPass.setUniform("frxu_lod", this.lod);
            renderPass.setUniform("frxu_layer", this.layer);
            renderPass.setUniform("frxu_frameProjectionMatrix", new Matrix4f().ortho2D(0, w, 0, h));

            // assuming that active texture unit is GL_TEXTURE0,
            // if we couldn't find first sampler location,
            // then attach first texture to the texture unit GL_TEXTURE0.
            // compat with canvas, for cases like this:
            // https://github.com/ambrosia13/ForgetMeNot-Shaders/commit/4eaa1e0f3bec07f265c504d760cccf2676c8fef5
            /*if (samplers.size() > 0 && !this.program.samplerExists(samplers.get(0))) {
                this.textures.get(0).ifPresent(texture -> texture.bind());
            }*/

            renderPass.setVertexBuffer(0, vertexBuffer);
            renderPass.setIndexBuffer(indexBuffer, autoStorageIndexBuffer.type());
            renderPass.drawIndexed(0, 6);
        }
    }

    static Optional<PassBase> load(
        JsonObject json,
        Function<String, Object> optionValueByName,
        Function<String, Optional<Framebuffer>> getOrLoadOptionalFramebuffer,
        Function<String, Program> getOrLoadProgram,
        Function<String, Optional<GlTexture>> getOrLoadPipelineOrResourcepackTexture
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

        Program program = getOrLoadProgram.apply(programName);
        Objects.nonNull(program);

        List<Optional<? extends GlTexture>> textures = new ArrayList<>();
        for (String s : JanksonUtils.listOfStrings(json, "samplerImages")) {
            textures.add(getOrLoadPipelineOrResourcepackTexture.apply(s));
        }

        int size = json.getInt("size", 0);
        Vector2i extent = new Vector2i(
            json.getInt("width", size),
            json.getInt("height", size)
        );
        int lod = json.getInt("lod", 0);
        int layer = json.getInt("layer", 0);

        return Optional.of(new Pass(passName, framebuffer.get(), program, textures, extent, lod, layer));
    }

    static class FREXClear extends PassBase {

        final String name;
        final Framebuffer framebuffer;

        FREXClear(String name, Framebuffer framebuffer) {
            this.name = name;
            this.framebuffer = framebuffer;
        }

        @Override
        public void apply(Matrix4f view, Matrix4f projection) {
            framebuffer.bindAndClearFully();
        }

    };

}
