package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.KHRDebug;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.GFX;
import fewizz.canpipe.mixininterface.GameRendererAccessor;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ShaderManager.CompilationException;
import net.minecraft.client.renderer.ShaderProgramConfig;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ProgramBase extends CompiledShaderProgram {

    private static final List<ShaderProgramConfig.Uniform> DEFAULT_UNIFORMS = List.of(
        new ShaderProgramConfig.Uniform("canpipe_light0Direction", "float", 3, List.of(0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("canpipe_light1Direction", "float", 3, List.of(0.0F, 0.0F, 0.0F)),

        // view.glsl
        new ShaderProgramConfig.Uniform("frx_cameraPos", "float", 3, List.of(0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("frx_cameraView", "float", 3, List.of(0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("frx_lastCameraPos", "float", 3, List.of(0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("frx_modelToWorld", "float", 4, List.of(0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("canpipe_originType", "int", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("canpipe_modelToCamera", "float", 3, List.of(0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("frx_viewMatrix", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("frx_lastViewMatrix", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("frx_projectionMatrix", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("frx_lastProjectionMatrix", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("frx_shadowViewMatrix", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("canpipe_shadowProjectionMatrix_0", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("canpipe_shadowProjectionMatrix_1", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("canpipe_shadowProjectionMatrix_2", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("canpipe_shadowProjectionMatrix_3", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("canpipe_shadowCenter_0", "float", 4, List.of(0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("canpipe_shadowCenter_1", "float", 4, List.of(0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("canpipe_shadowCenter_2", "float", 4, List.of(0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("canpipe_shadowCenter_3", "float", 4, List.of(0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("frx_fogStart", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frx_fogEnd", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("canpipe_screenSize", "float", 2, List.of(0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("frx_viewDistance", "float", 1, List.of(0.0F)),

        // player.glsl
        new ShaderProgramConfig.Uniform("frx_eyePos", "float", 3, List.of(0.0F, 0.0F, 0.0F)),

        // world.glsl
        new ShaderProgramConfig.Uniform("canpipe_renderFrames", "int", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("canpipe_fixedOrDayTime", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frx_renderSeconds", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frx_worldDay", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frx_worldTime", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frx_skyLightVector", "float", 3, List.of(0.0F, 1.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("frx_skyAngleRadians", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("canpipe_worldFlags", "int", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("canpipe_weatherGradients", "float", 4, List.of(0.0F, 0.0F, 0.0F, 0.0F)),

        // fog.glsl
        new ShaderProgramConfig.Uniform("frx_fogColor", "float", 4, List.of(0.0F, 0.0F, 0.0F, 0.0F))
    );

    final ResourceLocation location;
    final Map<Integer, Integer> samplerTargetByID = new Int2IntArrayMap();
    public final Set<Uniform> manuallyAppliedUniforms = new HashSet<>();

    public final Uniform
        FRX_MODEL_TO_WORLD,
        FRX_RENDER_FRAMES,
        FRX_CAMERA_POS,
        FRX_CAMERA_VIEW,
        FRX_LAST_CAMERA_POS,
        CANPIPE_ORIGIN_TYPE,
        FRX_LAST_VIEW_MATRIX,
        FRX_LAST_PROJECTION_MATRIX,
        FRX_SHADOW_VIEW_MATRIX,
        CANPIPE_SHADOW_PROJECTION_MATRIX_0,
        CANPIPE_SHADOW_PROJECTION_MATRIX_1,
        CANPIPE_SHADOW_PROJECTION_MATRIX_2,
        CANPIPE_SHADOW_PROJECTION_MATRIX_3,
        CANPIPE_SHADOW_CENTER_0,
        CANPIPE_SHADOW_CENTER_1,
        CANPIPE_SHADOW_CENTER_2,
        CANPIPE_SHADOW_CENTER_3,
        FRX_VIEW_DISTANCE,
        FRX_EYE_POS,
        CANPIPE_FIXED_OR_DAY_TIME,
        FRX_RENDER_SECONDS,
        FRX_WORLD_DAY,
        FRX_WORLD_TIME,
        FRX_SKY_LIGHT_VECTOR,
        FRX_SKY_ANGLE_RADIANS,
        CANPIPE_WORLD_FLAGS,
        CANPIPE_WEATHER_GRADIENTS,
        FRX_FOG_COLOR;

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

        this.FRX_MODEL_TO_WORLD = getUniform("frx_modelToWorld");  // non-manual
        this.FRX_RENDER_FRAMES = getManallyAppliedUniform("canpipe_renderFrames");
        this.FRX_CAMERA_POS = getManallyAppliedUniform("frx_cameraPos");
        this.FRX_CAMERA_VIEW = getManallyAppliedUniform("frx_cameraView");
        this.FRX_LAST_CAMERA_POS = getManallyAppliedUniform("frx_lastCameraPos");
        this.CANPIPE_ORIGIN_TYPE = getUniform("canpipe_originType");  // non-manual
        this.FRX_LAST_VIEW_MATRIX = getManallyAppliedUniform("frx_lastViewMatrix");
        this.FRX_LAST_PROJECTION_MATRIX = getManallyAppliedUniform("frx_lastProjectionMatrix");
        this.FRX_SHADOW_VIEW_MATRIX = getManallyAppliedUniform("frx_shadowViewMatrix");
        this.CANPIPE_SHADOW_PROJECTION_MATRIX_0 = getManallyAppliedUniform("canpipe_shadowProjectionMatrix_0");
        this.CANPIPE_SHADOW_PROJECTION_MATRIX_1 = getManallyAppliedUniform("canpipe_shadowProjectionMatrix_1");
        this.CANPIPE_SHADOW_PROJECTION_MATRIX_2 = getManallyAppliedUniform("canpipe_shadowProjectionMatrix_2");
        this.CANPIPE_SHADOW_PROJECTION_MATRIX_3 = getManallyAppliedUniform("canpipe_shadowProjectionMatrix_3");
        this.CANPIPE_SHADOW_CENTER_0 = getManallyAppliedUniform("canpipe_shadowCenter_0");
        this.CANPIPE_SHADOW_CENTER_1 = getManallyAppliedUniform("canpipe_shadowCenter_1");
        this.CANPIPE_SHADOW_CENTER_2 = getManallyAppliedUniform("canpipe_shadowCenter_2");
        this.CANPIPE_SHADOW_CENTER_3 = getManallyAppliedUniform("canpipe_shadowCenter_3");
        this.FRX_VIEW_DISTANCE = getManallyAppliedUniform("frx_viewDistance");
        this.FRX_EYE_POS = getManallyAppliedUniform("frx_eyePos");
        this.CANPIPE_FIXED_OR_DAY_TIME = getManallyAppliedUniform("canpipe_fixedOrDayTime");
        this.FRX_RENDER_SECONDS = getManallyAppliedUniform("frx_renderSeconds");
        this.FRX_WORLD_DAY = getManallyAppliedUniform("frx_worldDay");
        this.FRX_WORLD_TIME = getManallyAppliedUniform("frx_worldTime");
        this.FRX_SKY_LIGHT_VECTOR = getManallyAppliedUniform("frx_skyLightVector");
        this.FRX_SKY_ANGLE_RADIANS = getManallyAppliedUniform("frx_skyAngleRadians");
        this.CANPIPE_WORLD_FLAGS = getManallyAppliedUniform("canpipe_worldFlags");
        this.CANPIPE_WEATHER_GRADIENTS = getManallyAppliedUniform("canpipe_weatherGradients");
        this.FRX_FOG_COLOR = getManallyAppliedUniform("frx_fogColor");

        GFX.glObjectLabel(KHRDebug.GL_PROGRAM, getProgramId(), location.toString());
    }

    private Uniform getManallyAppliedUniform(String name) {
        Uniform u = this.getUniform(name);
        if (u != null) {
            this.manuallyAppliedUniforms.add(u);
        }
        return u;
    }

    @Override
    public Uniform getUniform(String name) {
        // :evil:
        if (name.equals("ModelViewMat")) { name = "frx_viewMatrix"; }
        if (name.equals("ProjMat")) { name = "frx_projectionMatrix"; }
        if (name.equals("FogStart")) { name = "frx_fogStart"; }
        if (name.equals("FogEnd")) { name = "frx_fogEnd"; }
        if (name.equals("ScreenSize")) { name = "canpipe_screenSize"; }
        if (name.equals("ModelOffset")) { name = "canpipe_modelToCamera"; }
        if (name.equals("Light0_Direction")) { name = "canpipe_light0Direction"; }
        if (name.equals("Light1_Direction")) { name = "canpipe_light1Direction"; }

        return super.getUniform(name);
    }

    public void setFREXUniforms() {
        Minecraft mc = Minecraft.getInstance();
        Pipeline p = Pipelines.getCurrent();

        GameRendererAccessor gra = (GameRendererAccessor) mc.gameRenderer;
        GlStateManager._glUseProgram(this.getProgramId());

        if (this.FRX_CAMERA_POS != null) {
            this.FRX_CAMERA_POS.set(mc.gameRenderer.getMainCamera().getPosition().toVector3f());
            this.FRX_CAMERA_POS.upload();
        }
        if (this.FRX_CAMERA_VIEW != null) {
            var cam = mc.gameRenderer.getMainCamera();
            this.FRX_CAMERA_VIEW.set(Vec3.directionFromRotation(cam.getXRot(), cam.getYRot()).toVector3f());
            this.FRX_CAMERA_VIEW.upload();
        }
        if (this.FRX_LAST_CAMERA_POS != null) {
            this.FRX_LAST_CAMERA_POS.set(gra.canpipe_getLastCameraPos());
            this.FRX_LAST_CAMERA_POS.upload();
        }
        if (this.FRX_LAST_VIEW_MATRIX != null) {
            this.FRX_LAST_VIEW_MATRIX.set(gra.canpipe_getLastViewMatrix());
            this.FRX_LAST_VIEW_MATRIX.upload();
        }
        if (this.FRX_LAST_PROJECTION_MATRIX != null) {
            this.FRX_LAST_PROJECTION_MATRIX.set(gra.canpipe_getLastProjectionMatrix());
            this.FRX_LAST_PROJECTION_MATRIX.upload();
        }
        if (this.FRX_SHADOW_VIEW_MATRIX != null) {
            this.FRX_SHADOW_VIEW_MATRIX.set(gra.canpipe_getShadowViewMatrix());
            this.FRX_SHADOW_VIEW_MATRIX.upload();
        }
        if (this.CANPIPE_SHADOW_PROJECTION_MATRIX_0 != null) {
            this.CANPIPE_SHADOW_PROJECTION_MATRIX_0.set(gra.canpipe_getShadowProjectionMatrices()[0]);
            this.CANPIPE_SHADOW_PROJECTION_MATRIX_0.upload();
        }
        if (this.CANPIPE_SHADOW_PROJECTION_MATRIX_1 != null) {
            this.CANPIPE_SHADOW_PROJECTION_MATRIX_1.set(gra.canpipe_getShadowProjectionMatrices()[1]);
            this.CANPIPE_SHADOW_PROJECTION_MATRIX_1.upload();
        }
        if (this.CANPIPE_SHADOW_PROJECTION_MATRIX_2 != null) {
            this.CANPIPE_SHADOW_PROJECTION_MATRIX_2.set(gra.canpipe_getShadowProjectionMatrices()[2]);
            this.CANPIPE_SHADOW_PROJECTION_MATRIX_2.upload();
        }
        if (this.CANPIPE_SHADOW_PROJECTION_MATRIX_3 != null) {
            this.CANPIPE_SHADOW_PROJECTION_MATRIX_3.set(gra.canpipe_getShadowProjectionMatrices()[3]);
            this.CANPIPE_SHADOW_PROJECTION_MATRIX_3.upload();
        }
        if (this.CANPIPE_SHADOW_CENTER_0 != null) {
            this.CANPIPE_SHADOW_CENTER_0.set(gra.canpipe_getShadowCenters()[0]);
            this.CANPIPE_SHADOW_CENTER_0.upload();
        }
        if (this.CANPIPE_SHADOW_CENTER_1 != null) {
            this.CANPIPE_SHADOW_CENTER_1.set(gra.canpipe_getShadowCenters()[1]);
            this.CANPIPE_SHADOW_CENTER_1.upload();
        }
        if (this.CANPIPE_SHADOW_CENTER_2 != null) {
            this.CANPIPE_SHADOW_CENTER_2.set(gra.canpipe_getShadowCenters()[2]);
            this.CANPIPE_SHADOW_CENTER_2.upload();
        }
        if (this.CANPIPE_SHADOW_CENTER_3 != null) {
            this.CANPIPE_SHADOW_CENTER_3.set(gra.canpipe_getShadowCenters()[3]);
            this.CANPIPE_SHADOW_CENTER_3.upload();
        }
        if (this.FRX_VIEW_DISTANCE != null) {
            this.FRX_VIEW_DISTANCE.set(mc.options.renderDistance().get() * 16.0F);
            this.FRX_VIEW_DISTANCE.upload();
        }
        if (this.FRX_EYE_POS != null) {
            this.FRX_EYE_POS.set(mc.player.position().toVector3f());
            this.FRX_EYE_POS.upload();
        }

        // world
        if (this.FRX_RENDER_FRAMES != null) {
            this.FRX_RENDER_FRAMES.set(gra.canpipe_getFrame());
            this.FRX_RENDER_FRAMES.upload();
        }
        if (this.FRX_RENDER_SECONDS != null) {
            this.FRX_RENDER_SECONDS.set(gra.canpipe_getRenderSeconds());
            this.FRX_RENDER_SECONDS.upload();
        }
        if (this.CANPIPE_FIXED_OR_DAY_TIME != null) {
            long ticks = mc.level.dimensionType().fixedTime().orElse(mc.level.getDayTime());
            this.CANPIPE_FIXED_OR_DAY_TIME.set((ticks % 24000L) / 24000.0F);
            this.CANPIPE_FIXED_OR_DAY_TIME.upload();
        }
        if (this.FRX_WORLD_DAY != null) {
            this.FRX_WORLD_DAY.set(mc.level != null ? (mc.level.getDayTime() / 24000L) % 2147483647L : 0.0F);
            this.FRX_WORLD_DAY.upload();
        }
        if (this.FRX_WORLD_TIME != null) {
            this.FRX_WORLD_TIME.set(mc.level != null ? (mc.level.getDayTime() % 24000L) / 24000.0F : 0.0F);
            this.FRX_WORLD_TIME.upload();
        }
        if (this.FRX_MODEL_TO_WORLD != null) {
            // will be re-set for terrain in LevelRenderer.renderSectionLayer
            // should be zero for everything else
            this.FRX_MODEL_TO_WORLD.set(0.0F, 0.0F, 0.0F);
        }
        if (this.FRX_SKY_LIGHT_VECTOR != null) {
            this.FRX_SKY_LIGHT_VECTOR.set(p.getSunOrMoonDir(mc.level, new Vector3f()));
            this.FRX_SKY_LIGHT_VECTOR.upload();
        }
        if (this.FRX_SKY_ANGLE_RADIANS != null) {
            this.FRX_SKY_ANGLE_RADIANS.set(mc.level.getSunAngle(0.0F));
            this.FRX_SKY_ANGLE_RADIANS.upload();
        }
        if (this.CANPIPE_WORLD_FLAGS != null) {
            int value = mc.level.dimensionType().hasSkyLight() ? 1 : 0;

            int dimension = 3;
            if (mc.level.dimension() == Level.OVERWORLD) {
                dimension = 0;
            }
            if (mc.level.dimension() == Level.NETHER) {
                dimension = 1;
            }
            if (mc.level.dimension() == Level.END) {
                dimension = 2;
            }
            value |= dimension << 1;

            value |= (mc.level.isRaining() ? 1 : 0) << 3;
            value |= (mc.level.isThundering() ? 1 : 0) << 4;
            value |= (mc.level.effects().constantAmbientLight() ? 1 : 0) << 5;

            this.CANPIPE_WORLD_FLAGS.set(value);
            this.CANPIPE_WORLD_FLAGS.upload();
        }
        if (this.CANPIPE_WEATHER_GRADIENTS != null) {
            this.CANPIPE_WEATHER_GRADIENTS.set(
                mc.level.getRainLevel(0),
                mc.level.getThunderLevel(0),
                mc.level.getRainLevel(0),    // TODO: smoothed
                mc.level.getThunderLevel(0)  // TODO: smoothed
            );
            this.CANPIPE_WEATHER_GRADIENTS.upload();
        }

        // fog.glsl
        if (this.FRX_FOG_COLOR != null) {
            this.FRX_FOG_COLOR.set(
                FogRenderer.computeFogColor(
                    mc.gameRenderer.getMainCamera(),
                    0,
                    mc.level, mc.options.getEffectiveRenderDistance(),
                    mc.gameRenderer.getDarkenWorldAmount(0)
                )
            );
            this.FRX_FOG_COLOR.upload();
        }

        GlStateManager._glUseProgram(0);
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
     * because vanilla supports only {@link GL33C#GL_TEXTURE_2D} texture target
    */
    public void onTextureBindOnApply(int textureID) {
        GFX.glBindTexture(this.samplerTargetByID.getOrDefault(textureID, GL33C.GL_TEXTURE_2D), textureID);
    }

    /**
     * Called on {@link CompiledShaderProgram#clear}
     * for particular texture/active texture.
     * We need this, because
     * <a href="https://community.khronos.org/t/binding-different-targets-to-same-unit/76935">
     * different targets could be bound to same texture unit
     * </a>.
     * Vanilla doesn't know about texture targets other that {@link GL33C#GL_TEXTURE_2D}
    */
    public void onClearSampler(int textureID, int textureUnit) {
        int target = samplerTargetByID.getOrDefault(textureID, GL33C.GL_TEXTURE_2D);
        if (target != GL33C.GL_TEXTURE_2D) {
            GlStateManager._activeTexture(GL33C.GL_TEXTURE0 + textureUnit);
            GFX.glBindTexture(target, textureID);  // unbind non-TEXTURE_2D
        }
    }

}
