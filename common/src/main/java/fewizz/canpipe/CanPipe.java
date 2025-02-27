package fewizz.canpipe;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import blue.endless.jankson.Jankson;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.MaterialProgram;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;

public class CanPipe {
    public static final String MOD_ID = "canpipe";
    public static final Logger LOGGER = LoggerFactory.getLogger("can-pipe");
    public static final Jankson JANKSON = Jankson.builder().build();

    // TODO remove if unused
    public static final ShaderProgram BLIT_DEPTH_PROGRAM = new ShaderProgram(
        ResourceLocation.fromNamespaceAndPath("canpipe", "blit_depth"),
        DefaultVertexFormat.POSITION,
        ShaderDefines.EMPTY
    );

    public static Path getCompilationErrorsDirPath() {
        Minecraft mc = Minecraft.getInstance();
        return mc.gameDirectory.toPath().resolve("can-pipe-compilation-errors");
    }

    public static Path getConfigurationFilePath() {
        Minecraft mc = Minecraft.getInstance();
        return mc.gameDirectory.toPath().resolve("config/can-pipe.json");
    }

    public static class VertexFormatElements {

        public static final VertexFormatElement
            MATERIAL_FLAGS = VertexFormatElement.register(
                6, 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.UV, 1
            ),  // UV, because it uses vertexAttrib *I* Pointer in this case
            AO = VertexFormatElement.register(
                7, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 1
            ),
            SPRITE_INDEX = VertexFormatElement.register(
                8, 0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.UV, 1
            ),  // UV, because it uses vertexAttrib *I* Pointer in this case
            MATERIAL_INDEX = VertexFormatElement.register(
                9, 0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.UV, 1
            ),
            TANGENT = VertexFormatElement.register(
                10, 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.NORMAL, 4
            );

    }

    public class VertexFormats {

        public static final VertexFormat BLOCK = VertexFormat.builder()
            /*  0 */.add("Position", VertexFormatElement.POSITION)
            /* 12 */.add("Color", VertexFormatElement.COLOR)
            /* 16 */.add("UV0", VertexFormatElement.UV0)
            /* 24 */.add("UV2", VertexFormatElement.UV2)
            /* 28 */.add("Normal", VertexFormatElement.NORMAL)
            /* 31 */.add("MaterialFlags", CanPipe.VertexFormatElements.MATERIAL_FLAGS)
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
            .add("MaterialFlags", CanPipe.VertexFormatElements.MATERIAL_FLAGS)
            .add("SpriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            .add("MaterialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            .add("Tangent", CanPipe.VertexFormatElements.TANGENT)
            .build();

        public static final VertexFormat PARTICLE = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .add("MaterialFlags", CanPipe.VertexFormatElements.MATERIAL_FLAGS)
            .add("SpriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            .add("MaterialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            .add("Tangent", CanPipe.VertexFormatElements.TANGENT)
            .build();

    }

    public static class RenderStateShards  {

        public static class MaterialProgramStateShard extends RenderStateShard.ShaderStateShard {

            final float alphaCutout;
            final Supplier<Integer> renderTargetIndexGetter;

            public MaterialProgramStateShard(
                RenderStateShard.ShaderStateShard original,
                Supplier<Integer> renderTargetIndexGetter,
                float alphaCutout
            ) {
                super(original.shader.get());
                this.renderTargetIndexGetter = renderTargetIndexGetter;
                this.alphaCutout = alphaCutout;
            }

            @Override
            public void setupRenderState() {
                Pipeline p = Pipelines.getCurrent();
                if (p == null) {
                    super.setupRenderState();
                    return;
                }

                VertexFormat originalFormat = this.shader.get().vertexFormat();
                VertexFormat replacedFormat = ((Supplier<VertexFormat>) () -> {
                    if (originalFormat == DefaultVertexFormat.BLOCK) {
                        return VertexFormats.BLOCK;
                    }
                    if (originalFormat == DefaultVertexFormat.NEW_ENTITY) {
                        return VertexFormats.NEW_ENTITY;
                    }
                    if (originalFormat == DefaultVertexFormat.PARTICLE) {
                        return VertexFormats.PARTICLE;
                    }
                    throw new RuntimeException("Couldn't replace vertex format");
                }).get();

                Minecraft mc = Minecraft.getInstance();

                MaterialProgram program =
                    ((LevelRendererExtended) mc.levelRenderer).canpipe_getIsRenderingShadows() ?
                    p.shadowPrograms.get(replacedFormat) :
                    p.materialPrograms.get(replacedFormat);

                RenderSystem.setShader(program);

                if (program.CANPIPE_RENDER_TARGET != null) {
                    program.CANPIPE_RENDER_TARGET.set(this.renderTargetIndexGetter.get());
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
                var p = Pipelines.getCurrent();
                var mc = Minecraft.getInstance();

                // bind original framebuffer if
                if (
                    // no pipeline is active
                    p == null
                    // rendering shadows (shadows framebuffer overrides mc.mainRenderTarget)
                    || ((LevelRendererExtended) mc.levelRenderer).canpipe_getIsRenderingShadows()
                ) {
                    original.setupRenderState();
                    return;
                }

                framebufferGetter.apply(p).bindWrite(false);
            }

        }

    }

    public static void trap() {
        int i = 0;
    }

}