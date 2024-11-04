package fewizz.canpipe.pipeline;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.lwjgl.opengl.GL33C;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;

public class Pass {

    static class FREXClear extends Pass {

        FREXClear(String name, Framebuffer framebuffer) {
            super(name, framebuffer, null, null, null, -1, -1);
        }

        @Override
        void apply(Matrix4f view, Matrix4f projection) {
            framebuffer.clearFully();
        }

    };

    final String name;
    final Framebuffer framebuffer;
    final Program program;
    final List<AbstractTexture> samplers;
    final Vector2i extent;
    final int lod;
    final int layer;

    Pass(
        String name,
        Framebuffer framebuffer,
        Program program,
        List<AbstractTexture> samplers,
        Vector2i extent,
        int lod, int layer
    ) {
        this.name = name;
        this.framebuffer = framebuffer;
        this.program = program;
        this.samplers = samplers;
        this.extent = extent;
        this.lod = lod;
        this.layer = layer;
    }

    void apply(Matrix4f view, Matrix4f projection) {
        Minecraft mc = Minecraft.getInstance();

        int w = this.extent.x;
        int h = this.extent.y;

        if (w == 0) w = mc.getMainRenderTarget().width;
        if (h == 0) h = mc.getMainRenderTarget().height;

        w >>= this.lod;
        h >>= this.lod;

        RenderSystem.viewport(0, 0, (int) w, (int) h);

        program.setDefaultUniforms(view, projection, w, h, this.lod);
        program.bindExpectedSamplers(samplers);
        program.apply();

        framebuffer.bindWrite(false);

        RenderSystem.depthFunc(GL33C.GL_ALWAYS);

        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F);
        bufferBuilder.addVertex(1.0F, 0.0F, 0.0F).setUv(1.0F, 0.0F);
        bufferBuilder.addVertex(1.0F, 1.0F, 0.0F).setUv(1.0F, 1.0F);
        bufferBuilder.addVertex(0.0F, 1.0F, 0.0F).setUv(0.0F, 1.0F);
        BufferUploader.draw(bufferBuilder.buildOrThrow());

        RenderSystem.depthFunc(GL33C.GL_LEQUAL);

        framebuffer.unbindWrite();
        program.clear();
    }
}
