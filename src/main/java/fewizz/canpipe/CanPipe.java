package fewizz.canpipe;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import blue.endless.jankson.Jankson;
import fewizz.canpipe.mixin.RenderSystemAccessor;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.MaterialProgram;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;

public class CanPipe {
    public static final String MOD_ID = "canpipe";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Jankson JANKSON = Jankson.builder().build();

    public static class VertexFormatElements {

        public static final VertexFormatElement AO =
            VertexFormatElement.register(
                6, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 1
            );

        public static final VertexFormatElement SPRITE_INDEX =
            VertexFormatElement.register(
                7, 0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.UV, 1
            );  // UV, because it uses vertexAttrib *I* Pointer in this case

        public static final VertexFormatElement MATERIAL_INDEX =
            VertexFormatElement.register(
                8, 0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.UV, 1
            );

        public static final VertexFormatElement TANGENT =
            VertexFormatElement.register(
                9, 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.NORMAL, 4
            );

    }

    public class VertexFormats {

        public static final VertexFormat BLOCK = VertexFormat.builder()
            /*  0 */.add("Position", VertexFormatElement.POSITION)
            /* 12 */.add("Color", VertexFormatElement.COLOR)
            /* 16 */.add("UV0", VertexFormatElement.UV0)
            /* 24 */.add("UV2", VertexFormatElement.UV2)
            /* 28 */.add("Normal", VertexFormatElement.NORMAL)
            .padding(1)
            /* 32 */.add("AO", CanPipe.VertexFormatElements.AO)
            /* 36 */.add("SpriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            /* 40 */.add("MaterialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            /* 44 */.add("Tangent", CanPipe.VertexFormatElements.TANGENT)
            .build();

        public static final VertexFormat NEW_ENTITY = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV1", VertexFormatElement.UV1)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .padding(1)
            .add("SpriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            .add("MaterialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            .add("Tangent", CanPipe.VertexFormatElements.TANGENT)
            .build();

        public static final VertexFormat PARTICLE = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .padding(1)
            .add("SpriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            .add("MaterialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            .add("Tangent", CanPipe.VertexFormatElements.TANGENT)
            .build();

        public static VertexFormat replace(VertexFormat format) {
            if (format == DefaultVertexFormat.BLOCK) return VertexFormats.BLOCK;
            if (format == DefaultVertexFormat.NEW_ENTITY) return VertexFormats.NEW_ENTITY;
            if (format == DefaultVertexFormat.PARTICLE) return VertexFormats.PARTICLE;
            throw new RuntimeException("Couldn't replace vertex format");
        }

        public static boolean isCanpipeFormat(VertexFormat format) {
            if (format == BLOCK || format == NEW_ENTITY || format == PARTICLE) {
                return true;
            }
            return false;
        }

    }

    public static class RenderStateShards  {

        public static class MaterialProgramStateShard extends RenderStateShard.ShaderStateShard {

            final float alphaCutout;
            final int renderTargetIndex;

            public MaterialProgramStateShard(
                RenderStateShard.ShaderStateShard original,
                int renderTargetIndex,
                float alphaCutout
            ) {
                super(original.shader.get());
                this.renderTargetIndex = renderTargetIndex;
                this.alphaCutout = alphaCutout;
            }

            @Override
            public void setupRenderState() {
                Pipeline p = Pipelines.getCurrent();
                if (p == null) {
                    super.setupRenderState();
                    return;
                }

                VertexFormat format = this.shader.get().vertexFormat();
                format = VertexFormats.replace(format);
                Minecraft mc = Minecraft.getInstance();

                MaterialProgram program =
                    ((LevelRendererExtended) mc.levelRenderer).getIsRenderingShadow() ?
                    p.shadowPrograms.get(format) :
                    p.materialPrograms.get(format);

                RenderSystemAccessor.canpipe_invokeAssertOnRenderThread();
                RenderSystemAccessor.canpipe_setShader(program);

                if (program.CANPIPE_RENDER_TARGET != null) {
                    // TODO this is ugly, i know
                    int actualTarget = 0;
                    if (this.renderTargetIndex == 1 && mc.levelRenderer.getTranslucentTarget() != null) {
                        actualTarget = 1;
                    } else
                    if (this.renderTargetIndex == 2 && mc.levelRenderer.getItemEntityTarget() != null) {
                        actualTarget = 2;
                    } else
                    if (this.renderTargetIndex == 3 && mc.levelRenderer.getParticlesTarget() != null) {
                        actualTarget = 3;
                    }
                    program.CANPIPE_RENDER_TARGET.set(actualTarget);
                }

                if (program.CANPIPE_ALPHA_CUTOUT != null) {
                    program.CANPIPE_ALPHA_CUTOUT.set(this.alphaCutout);
                }
            }

        }

        public static class OutputStateShard extends RenderStateShard.OutputStateShard {

            final RenderStateShard.OutputStateShard original;
            final Function<Pipeline, Framebuffer> framebufferGetter;

            public OutputStateShard(
                String name,
                RenderStateShard.OutputStateShard original,
                Function<Pipeline, Framebuffer> framebufferGetter
            ) {
                super(name, () -> {}, () -> {});
                this.original = original;
                this.framebufferGetter = framebufferGetter;
            }

            @Override
            public void setupRenderState() {
                Pipeline p = Pipelines.getCurrent();
                if (p == null) {
                    original.setupRenderState();
                    return;
                }

                var mc = Minecraft.getInstance();

                Framebuffer framebuffer =
                    ((LevelRendererExtended) mc.levelRenderer).getIsRenderingShadow() ?
                    p.framebuffers.get(p.skyShadows.framebufferName()) :
                    framebufferGetter.apply(p);

                framebuffer.bindWrite(false);
            }

        }

    }

}