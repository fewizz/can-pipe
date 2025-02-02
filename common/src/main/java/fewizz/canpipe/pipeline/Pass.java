package fewizz.canpipe.pipeline;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2i;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.CanPipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;

public class Pass extends PassBase {


    static class FREXClear extends PassBase {

        final String name;
        final Framebuffer framebuffer;

        FREXClear(String name, Framebuffer framebuffer) {
            this.name = name;
            this.framebuffer = framebuffer;
        }

        @Override
        public void apply(Matrix4f view, Matrix4f projection) {
            framebuffer.clearFully();
        }

    };

    final String name;
    final Framebuffer framebuffer;
    final Program program;
    final List<? extends AbstractTexture> textures;
    final Vector2i extent;
    final int lod;
    final int layer;

    Pass(
        String name,
        Framebuffer framebuffer,
        Program program,
        List<? extends AbstractTexture> textures,
        Vector2i extent,
        int lod,
        int layer
    ) {
        if (program.samplers.size() > textures.size()) {
            CanPipe.LOGGER.warn("Program "+program.location+" has more samplers than textures provided by pass "+name);
        }
        if (program.samplers.size() < textures.size()) {
            CanPipe.LOGGER.warn("Program "+program.location+" has less samplers than textures provided by pass "+name);
        }
        for (int i = 0; i < Math.min(program.samplers.size(), textures.size()); ++i) {
            String sampler = program.samplers.get(i);
            AbstractTexture texture = textures.get(i);
            if (texture == null && program.samplerExists(sampler)) {
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

        if (program.FRXU_SIZE != null) {
            program.FRXU_SIZE.set((int) w, (int) h);
        }
        if (program.FRXU_LOD != null) {
            program.FRXU_LOD.set(this.lod);
        }
        if (program.FRXU_LAYER != null) {
            program.FRXU_LAYER.set(this.layer);
        }
        if (program.FRXU_FRAME_PROJECTION_MATRIX != null) {
            program.FRXU_FRAME_PROJECTION_MATRIX.set(new Matrix4f().ortho2D(0, w, 0, h));
        }

        RenderSystem.viewport(0, 0, (int) w, (int) h);

        framebuffer.bindWrite(false);

        // program.setDefaultUniforms(view, projection, w, h, this.lod, this.layer);
        for (int i = 0; i < Math.min(program.samplers.size(), this.textures.size()); ++i) {
            String sampler = program.samplers.get(i);
            AbstractTexture texture = this.textures.get(i);
            if (texture == null) {  // Again, how is this possible?
                continue;
            }
            program.bindSampler(sampler, texture);
        }
        program.apply();

        // assuming that active texture unit is GL_TEXTURE0,
        // if we couldn't find first sampler location,
        // then attach first texture to the texture unit GL_TEXTURE0.
        // compat with canvas, for cases like this:
        // https://github.com/ambrosia13/ForgetMeNot-Shaders/commit/4eaa1e0f3bec07f265c504d760cccf2676c8fef5
        if (program.samplers.size() > 0 && !program.samplerExists(program.samplers.get(0))) {
            this.textures.get(0).bind();
        }

        RenderSystem.disableDepthTest();

        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F);
        bufferBuilder.addVertex(1.0F, 0.0F, 0.0F).setUv(1.0F, 0.0F);
        bufferBuilder.addVertex(1.0F, 1.0F, 0.0F).setUv(1.0F, 1.0F);
        bufferBuilder.addVertex(0.0F, 1.0F, 0.0F).setUv(0.0F, 1.0F);
        BufferUploader.draw(bufferBuilder.buildOrThrow());

        RenderSystem.enableDepthTest();

        framebuffer.unbindWrite();
        program.clear();
    }
}
