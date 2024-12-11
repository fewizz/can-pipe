package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.KHRDebug;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import fewizz.canpipe.mixininterface.GameRendererAccessor;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderManager.CompilationException;
import net.minecraft.client.renderer.ShaderProgramConfig;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;

public class ProgramBase extends CompiledShaderProgram {

    private static final List<ShaderProgramConfig.Uniform> DEFAULT_UNIFORMS = List.of(
        new ShaderProgramConfig.Uniform("canpipe_light0Direction", "float", 3, List.of(0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("canpipe_light1Direction", "float", 3, List.of(0.0F, 0.0F, 0.0F)),
        // view.glsl
        new ShaderProgramConfig.Uniform("frx_cameraPos", "float", 3, List.of(0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("frx_lastCameraPos", "float", 3, List.of(0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("frx_modelToWorld", "float", 4, List.of(0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("canpipe_originType", "int", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("canpipe_modelToCamera", "float", 3, List.of(0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("frx_viewMatrix", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("frx_lastViewMatrix", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("frx_projectionMatrix", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("frx_lastProjectionMatrix", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("frx_fogStart", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frx_fogEnd", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frx_fogColor", "float", 4, List.of(0.0F, 0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("canpipe_screenSize", "float", 2, List.of(0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("frx_viewDistance", "float", 1, List.of(0.0F)),

        // player.glsl
        new ShaderProgramConfig.Uniform("frx_eyePos", "float", 3, List.of(0.0F, 0.0F, 0.0F)),

        // world.glsl
        new ShaderProgramConfig.Uniform("canpipe_renderFrames", "int", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("canpipe_timeOfDay", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frx_renderSeconds", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frx_worldDay", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frx_worldTime", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frx_skyLightVector", "float", 3, List.of(0.0F, 1.0F, 0.0F))
    );

    final ResourceLocation location;
    final Map<Integer, Integer> samplerTargetByID = new Int2IntArrayMap();
    public final Uniform
        FRX_MODEL_TO_WORLD,
        FRX_RENDER_FRAMES,
        FRX_CAMERA_POS,
        FRX_LAST_CAMERA_POS,
        CANPIPE_ORIGIN_TYPE,
        FRX_LAST_VIEW_MATRIX,
        FRX_LAST_PROJECTION_MATRIX,
        FRX_VIEW_DISTANCE,
        FRX_EYE_POS,
        CANPIPE_TIME_OF_DAY,
        FRX_RENDER_SECONDS,
        FRX_WORLD_DAY,
        FRX_WORLD_TIME,
        FRX_SKY_LIGHT_VECTOR;

    ProgramBase(
        ResourceLocation location,
        VertexFormat vertexFormat,
        List<String> internalSamplers,
        List<String> samplers,
        List<ShaderProgramConfig.Uniform> uniforms,
        Shader vertexShader,
        Shader fragmentShader
    ) throws CompilationException, IOException {
        super(CompiledShaderProgram.link(vertexShader, fragmentShader, vertexFormat).getProgramId());
        this.location = location;

        setupUniforms(
            Streams.concat(DEFAULT_UNIFORMS.stream(), uniforms.stream()).toList(),
            Streams.concat(internalSamplers.stream(), samplers.stream()).map(s -> new ShaderProgramConfig.Sampler(s)).toList()
        );

        this.FRX_MODEL_TO_WORLD = getUniform("frx_modelToWorld");
        this.FRX_RENDER_FRAMES = getUniform("canpipe_renderFrames");
        this.FRX_CAMERA_POS = getUniform("frx_cameraPos");
        this.FRX_LAST_CAMERA_POS = getUniform("frx_lastCameraPos");
        this.CANPIPE_ORIGIN_TYPE = getUniform("canpipe_originType");
        this.FRX_LAST_VIEW_MATRIX = getUniform("frx_lastViewMatrix");
        this.FRX_LAST_PROJECTION_MATRIX = getUniform("frx_lastProjectionMatrix");
        this.FRX_VIEW_DISTANCE = getUniform("frx_viewDistance");
        this.FRX_EYE_POS = getUniform("frx_eyePos");
        this.CANPIPE_TIME_OF_DAY = getUniform("canpipe_timeOfDay");
        this.FRX_RENDER_SECONDS = getUniform("frx_renderSeconds");
        this.FRX_WORLD_DAY = getUniform("frx_worldDay");
        this.FRX_WORLD_TIME = getUniform("frx_worldTime");
        this.FRX_SKY_LIGHT_VECTOR = getUniform("frx_skyLightVector");

        KHRDebug.glObjectLabel(KHRDebug.GL_PROGRAM, getProgramId(), location.toString());
    }

    @Override
    public Uniform getUniform(String name) {
        // :evil:
        if (name.equals("ModelViewMat")) { name = "frx_viewMatrix"; }
        if (name.equals("ProjMat")) { name = "frx_projectionMatrix"; }
        if (name.equals("FogStart")) { name = "frx_fogStart"; }
        if (name.equals("FogEnd")) { name = "frx_fogEnd"; }
        if (name.equals("FogColor")) { name = "frx_fogColor"; }
        if (name.equals("ScreenSize")) { name = "canpipe_screenSize"; }
        if (name.equals("ModelOffset")) { name = "canpipe_modelToCamera"; }
        if (name.equals("Light0_Direction")) { name = "canpipe_light0Direction"; }
        if (name.equals("Light1_Direction")) { name = "canpipe_light1Direction"; }

        return super.getUniform(name);
    }

    @Override
    public void setDefaultUniforms(Mode mode, Matrix4f viewMatrix, Matrix4f projectionMatrix, Window window) {
        super.setDefaultUniforms(mode, viewMatrix, projectionMatrix, window);

        Minecraft mc = Minecraft.getInstance();
        Pipeline p = Pipelines.getCurrent();

        GameRendererAccessor gra = (GameRendererAccessor) mc.gameRenderer;

        if (this.FRX_CAMERA_POS != null) {
            this.FRX_CAMERA_POS.set(mc.gameRenderer.getMainCamera().getPosition().toVector3f());
        }
        if (this.FRX_LAST_CAMERA_POS != null) {
            this.FRX_LAST_CAMERA_POS.set(gra.canpipe_getLastCameraPos());
        }
        if (this.FRX_LAST_VIEW_MATRIX != null) {
            this.FRX_LAST_VIEW_MATRIX.set(gra.canpipe_getLastViewMatrix());
        }
        if (this.FRX_LAST_PROJECTION_MATRIX != null) {
            this.FRX_LAST_PROJECTION_MATRIX.set(gra.canpipe_getLastProjectionMatrix());
        }
        if (this.FRX_VIEW_DISTANCE != null) {
            this.FRX_VIEW_DISTANCE.set(mc.options.renderDistance().get() * 16.0F);
        }
        if (this.FRX_EYE_POS != null) {
            this.FRX_EYE_POS.set(mc.player.position().toVector3f());
        }

        // world
        if (this.FRX_RENDER_FRAMES != null) {
            this.FRX_RENDER_FRAMES.set(gra.canpipe_getFrame());
        }
        if (this.FRX_RENDER_SECONDS != null) {
            this.FRX_RENDER_SECONDS.set(gra.canpipe_getRenderSeconds());
        }
        if (this.CANPIPE_TIME_OF_DAY != null) {
            this.CANPIPE_TIME_OF_DAY.set(mc.level != null ? mc.level.getTimeOfDay(0.0F): 0.0F);
        }
        if (this.FRX_WORLD_DAY != null) {
            this.FRX_WORLD_DAY.set(mc.level != null ? (mc.level.getGameTime() / 24000L) % 2147483647L : 0.0F);
        }
        if (this.FRX_WORLD_TIME != null) {
            this.FRX_WORLD_TIME.set(mc.level != null ? (mc.level.getGameTime() % 24000.0F) / 24000.0F : 0.0F);
        }
        if (this.FRX_MODEL_TO_WORLD != null) {
            // will be re-set for terrain in LevelRenderer.renderSectionLayer
            // should be zero for everything else
            this.FRX_MODEL_TO_WORLD.set(0.0F, 0.0F, 0.0F);
        }
        if (this.FRX_SKY_LIGHT_VECTOR != null) {
            // 0.0 - noon, 0.5 - midnight
            float hourAngle = mc.level.getSunAngle(0.0F);
            float zenithAngle = p.skyShadows != null ? p.sky.defaultZenithAngle() : 0.0F;

            this.FRX_SKY_LIGHT_VECTOR.set(
                (float) (-Math.sin(hourAngle)),
                (float) ( Math.cos(hourAngle) *  Math.cos(zenithAngle)),
                (float) ( Math.cos(hourAngle) * -Math.sin(zenithAngle))
            );
        }
    }

    protected boolean samplerExists(String sampler) {
        for (int i = 0; i < this.samplers.size(); ++i) {
            if (this.samplers.get(i).name().equals(sampler)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Doesn't know about texture targets.
     * Use {@link ProgramBase#bindSampler(String, AbstractTexture)} instead.
     * */
    @Deprecated
    @Override
    public void bindSampler(String sampler, int textureID) {
        super.bindSampler(sampler, textureID);
    }

    public void bindSampler(String sampler, AbstractTexture texture) {
        this.bindSampler(sampler, texture.getId());
        int target = GL33C.GL_TEXTURE_2D;
        if (texture instanceof Texture t) {
            target = t.target;
        }
        this.samplerTargetByID.put(texture.getId(), target);
    }

    /**
     * Replaces {@link RenderSystem#bindTexture} on {@link CompiledShaderProgram#apply},
     * because vanilla supports only {@link GL11#GL_TEXTURE_2D} texture target
    */
    public void onApplyTextureBind(int textureID) {
        for (int target : List.of(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_2D_ARRAY, GL33C.GL_TEXTURE_CUBE_MAP, GL33C.GL_TEXTURE_3D)) {
            Texture.bind(0, target);  // Stupid AF, TODO
        }
        Texture.bind(textureID, this.samplerTargetByID.getOrDefault(textureID, GL33C.GL_TEXTURE_2D));
    }

}
