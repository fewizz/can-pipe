package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.KHRDebug;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
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

    static final List<ShaderProgramConfig.Uniform> DEFAULT_UNIFORMS = List.of(
        // view.glsl
        new ShaderProgramConfig.Uniform("frx_cameraPos", "float", 3, List.of(0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("frx_lastCameraPos", "float", 3, List.of(0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("frx_modelToWorld", "float", 4, List.of(0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("canpipe_originType", "int", 1, List.of(0.0F)),
        // new ShaderProgramConfig.Uniform("frx_modelToCamera", "float", 4, List.of(0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("canpipe_modelToCamera", "float", 3, List.of(0.0F, 0.0F, 0.0F)),  // takes it's value from vanilla uniform, "ModelOffset"
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
        new ShaderProgramConfig.Uniform("canpipe_gameTime", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("canpipe_renderFrames", "int", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frx_worldDay", "float", 1, List.of(0.0F))
    );

    public final Uniform FRX_MODEL_TO_WORLD;
    final Uniform FRX_RENDER_FRAMES;
    final Uniform FRX_CAMERA_POS;
    final Uniform FRX_LAST_CAMERA_POS;
    public final Uniform CANPIPE_ORIGIN_TYPE;
    final Uniform FRX_LAST_VIEW_MATRIX;
    final Uniform FRX_LAST_PROJECTION_MATRIX;
    final Uniform FRX_VIEW_DISTANCE;
    final Uniform FRX_EYE_POS;
    final Uniform FRX_WORLD_DAY;

    final Map<Integer, Integer> samplerTargets = new Int2IntArrayMap();
    final List<String> expectedSamplers;

    ProgramBase(
        ResourceLocation location,
        VertexFormat vertexFormat,
        List<ShaderProgramConfig.Sampler> samplers,
        List<ShaderProgramConfig.Uniform> uniforms,
        Shader vertexShader,
        Shader fragmentShader,
        List<String> expectedSamplers
    ) throws CompilationException, IOException {
        super(CompiledShaderProgram.link(vertexShader, fragmentShader, vertexFormat).getProgramId());

        setupUniforms(
            Streams.concat(
                DEFAULT_UNIFORMS.stream(),
                uniforms.stream()
            ).toList(),
            samplers
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
        this.FRX_WORLD_DAY = getUniform("frx_worldDay");

        this.expectedSamplers = expectedSamplers;

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
        if (name.equals("GameTime")) { name = "canpipe_gameTime"; }
        if (name.equals("ModelOffset")) { name = "canpipe_modelToCamera"; }

        return super.getUniform(name);
    }

    public void bindSampler(String name, AbstractTexture texture) {
        this.bindSampler(name, texture.getId());
        int target = GL33C.GL_TEXTURE_2D;
        if (texture instanceof Texture t) {
            target = t.target;
        }
        this.samplerTargets.put(texture.getId(), target);
    }

    public void bindExpectedSamplers(List<AbstractTexture> textures) {
        for (int i = 0; i < Math.min(expectedSamplers.size(), textures.size()); ++i) {
            String name = expectedSamplers.get(i);
            AbstractTexture texture = textures.get(i);
            this.bindSampler(name, texture);
        }
    }

    @Override
    public void setDefaultUniforms(Mode mode, Matrix4f view, Matrix4f projection, Window window) {
        super.setDefaultUniforms(mode, view, projection, window);

        Minecraft mc = Minecraft.getInstance();

        GameRendererAccessor gra = (GameRendererAccessor) mc.gameRenderer;

        if (this.FRX_RENDER_FRAMES != null) {
            this.FRX_RENDER_FRAMES.set(gra.canpipe_getFrame());
        }
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
        if (this.FRX_WORLD_DAY != null) {
            this.FRX_WORLD_DAY.set(mc.level != null ? mc.level.getGameTime() / 24000.0F : 0.0F);
        }
        if (this.FRX_MODEL_TO_WORLD != null) {
            // will be re-set for terrain in LevelRenderer.renderSectionLayer
            // should be zero for everything else
            this.FRX_MODEL_TO_WORLD.set(0.0F, 0.0F, 0.0F);
        }
    }

    public void onTexureBind(int id) {
        for (int target : List.of(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_2D_ARRAY, GL33C.GL_TEXTURE_CUBE_MAP, GL33C.GL_TEXTURE_3D)) {
            Texture.bind(0, target);  // Stupid AF, TODO
        }
        Texture.bind(id, this.samplerTargets.getOrDefault(id, GL33C.GL_TEXTURE_2D));
    }

}
