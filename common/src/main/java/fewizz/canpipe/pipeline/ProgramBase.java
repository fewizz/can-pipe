package fewizz.canpipe.pipeline;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryStack;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlShaderModule;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.light.Light;
import fewizz.canpipe.light.Lights;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.mixininterface.LightTextureExtended;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderManager.CompilationException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

public abstract class ProgramBase extends GlProgram {

    private static final List<RenderPipeline.UniformDescription> DEFAULT_UNIFORMS = List.of(
        // accessibility.glsl
        new RenderPipeline.UniformDescription("frx_fovEffects", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_distortionEffects", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_hideLightningFlashes", UniformType.INT),
        new RenderPipeline.UniformDescription("frx_darknessPulsing", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_highContrast", UniformType.INT),
        new RenderPipeline.UniformDescription("frx_damageTilt", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_glintStrength", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_glintSpeed", UniformType.FLOAT),

        // view.glsl
        new RenderPipeline.UniformDescription("frx_cameraPos", UniformType.VEC3),
        new RenderPipeline.UniformDescription("frx_cameraView", UniformType.VEC3),
        new RenderPipeline.UniformDescription("frx_lastCameraPos", UniformType.VEC3),
        new RenderPipeline.UniformDescription("frx_modelToWorld", UniformType.VEC4),
        new RenderPipeline.UniformDescription("canpipe_originType", UniformType.INT),
        new RenderPipeline.UniformDescription("canpipe_modelToCamera", UniformType.VEC3),
        new RenderPipeline.UniformDescription("frx_viewMatrix", UniformType.MATRIX4X4),
        new RenderPipeline.UniformDescription("frx_inverseViewMatrix", UniformType.MATRIX4X4),
        new RenderPipeline.UniformDescription("frx_lastViewMatrix", UniformType.MATRIX4X4),
        new RenderPipeline.UniformDescription("frx_projectionMatrix", UniformType.MATRIX4X4),
        new RenderPipeline.UniformDescription("frx_inverseProjectionMatrix", UniformType.MATRIX4X4),
        new RenderPipeline.UniformDescription("frx_lastProjectionMatrix", UniformType.MATRIX4X4),
        new RenderPipeline.UniformDescription("frx_inverseShadowViewMatrix", UniformType.MATRIX4X4),
        new RenderPipeline.UniformDescription("frx_shadowViewMatrix", UniformType.MATRIX4X4),
        new RenderPipeline.UniformDescription("canpipe_shadowCenter_0", UniformType.VEC4),
        new RenderPipeline.UniformDescription("canpipe_shadowCenter_1", UniformType.VEC4),
        new RenderPipeline.UniformDescription("canpipe_shadowCenter_2", UniformType.VEC4),
        new RenderPipeline.UniformDescription("canpipe_shadowCenter_3", UniformType.VEC4),
        new RenderPipeline.UniformDescription("frx_fogStart", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_fogEnd", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("canpipe_screenSize", UniformType.VEC2),
        new RenderPipeline.UniformDescription("frx_viewDistance", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_viewBrightness", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("canpipe_viewFlags", UniformType.INT),

        // player.glsl
        new RenderPipeline.UniformDescription("frx_effectModifier", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("canpipe_darknessFactor", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_eyePos", UniformType.VEC3),
        new RenderPipeline.UniformDescription("frx_eyeBrightness", UniformType.VEC2),
        new RenderPipeline.UniformDescription("frx_smoothedEyeBrightness", UniformType.VEC2),
        new RenderPipeline.UniformDescription("frx_heldLight", UniformType.VEC4),
        new RenderPipeline.UniformDescription("frx_heldLightInnerRadius", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_heldLightOuterRadius", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_playerMood", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("canpipe_playerFlags", UniformType.INT),
        new RenderPipeline.UniformDescription("canpipe_effectsFlags", UniformType.valueOf("IVEC2")),

        // world.glsl
        new RenderPipeline.UniformDescription("canpipe_renderFrames", UniformType.INT),
        new RenderPipeline.UniformDescription("canpipe_fixedOrDayTime", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_renderSeconds", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_worldDay", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_worldTime", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_skyLightVector", UniformType.VEC3),
        new RenderPipeline.UniformDescription("frx_moonSize", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_skyAngleRadians", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("canpipe_sunriseOrSunsetColor", UniformType.VEC3),
        new RenderPipeline.UniformDescription("frx_skyFlashStrength", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_ambientIntensity", UniformType.FLOAT),
        new RenderPipeline.UniformDescription("frx_emissiveColor", UniformType.VEC4),
        new RenderPipeline.UniformDescription("canpipe_worldFlags", UniformType.INT),
        new RenderPipeline.UniformDescription("canpipe_weatherGradients", UniformType.VEC4),

        // fog.glsl
        new RenderPipeline.UniformDescription("frx_fogColor", UniformType.VEC4),
        new RenderPipeline.UniformDescription("frx_fogEnabled", UniformType.INT)
    );

    public final Shader vertexShader;
    public final Shader fragmentShader;
    protected final List<String> samplersUniformNames;

    public final Uniform
        // accessibility.glsl
        FRX_FOV_EFFECTS,
        FRX_DISTORTION_EFFECTS,
        FRX_HIDE_LIGHTNING_FLASHES,
        FRX_DARKNESS_PULSING,
        FRX_HIGH_CONTRAST,
        FRX_DAMAGE_TILT,
        FRX_GLINT_STRENGTH,
        FRX_GLINT_SPEED,

        // view.glsl
        FRX_MODEL_TO_WORLD,
        CANPIPE_ORIGIN_TYPE,
        FRX_CAMERA_POS,
        FRX_CAMERA_VIEW,
        FRX_LAST_CAMERA_POS,
        FRX_INVERSE_VIEW_MATRIX,
        FRX_LAST_VIEW_MATRIX,
        FRX_INVERSE_PROJECTION_MATRIX,
        FRX_LAST_PROJECTION_MATRIX,
        FRX_INVERSE_SHADOW_VIEW_MATRIX,
        FRX_SHADOW_VIEW_MATRIX,
        CANPIPE_SHADOW_CENTER_0,
        CANPIPE_SHADOW_CENTER_1,
        CANPIPE_SHADOW_CENTER_2,
        CANPIPE_SHADOW_CENTER_3,
        FRX_VIEW_DISTANCE,
        FRX_VIEW_BRIGHTNESS,
        CANPIPE_VIEW_FLAGS,
        CANPIPE_SCREEN_SIZE,

        // player.glsl
        FRX_EFFECT_MODIFIER,
        CANPIPE_DARKNESS_FACTOR,
        FRX_EYE_POS,
        FRX_EYE_BRIGHTNESS,
        FRX_SMOOTHED_EYE_BRIGHTNESS,
        FRX_HELD_LIGHT,
        FRX_HELD_LIGHT_OUTER_RADIUS,
        FRX_HELD_LIGHT_INNER_RADIUS,
        FRX_PLAYER_MOOD,
        CANPIPE_PLAYER_FLAGS,
        CANPIPE_EFFECTS_FLAGS,

        // world.glsl
        CANPIPE_RENDER_FRAMES,
        CANPIPE_FIXED_OR_DAY_TIME,
        FRX_RENDER_SECONDS,
        FRX_WORLD_DAY,
        FRX_WORLD_TIME,
        FRX_MOON_SIZE,
        FRX_SKY_LIGHT_VECTOR,
        FRX_SKY_ANGLE_RADIANS,
        CANPIPE_SUNRISE_OR_SUNSET_COLOR,
        FRX_SKY_FLASH_STRENGTH,
        FRX_AMBIENT_INTENSITY,
        FRX_EMISSIVE_COLOR,
        CANPIPE_WORLD_FLAGS,
        CANPIPE_WEATHER_GRADIENTS,

        // fog.glsl
        FRX_FOG_COLOR,
        FRX_FOG_ENABLED;

    private static int _link(String name, GlShaderModule vertexShader, GlShaderModule fragmentShader, VertexFormat vertexFormat, String debugLabel) {
        try {
            return GlProgram.link(vertexShader, fragmentShader, vertexFormat, debugLabel).getProgramId();
        } catch (CompilationException e) {
            throw new RuntimeException("Couldn't link program \""+name+"\": "+e.getMessage(), e);
        }
    }

    ProgramBase(
        String name, VertexFormat vertexFormat,
        List<String> samplers, List<RenderPipeline.UniformDescription> uniforms,
        Shader vertexShader, Shader fragmentShader
    ) {
        super(ProgramBase._link(name, vertexShader, fragmentShader, vertexFormat, name), name);
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        samplers = new ArrayList<>(samplers);
        uniforms = Streams.concat(DEFAULT_UNIFORMS.stream(), uniforms.stream()).toList();

        /*
         * for cases when unform name in `programs` is misspelled, like in:
         * - Aerie v1.0.0 "copy" program (https://github.com/ambrosia13/Aerie-Shaders/pull/2),
         * - Forget-me-not v0.8.0 "depth_downsample" program (https://github.com/ambrosia13/ForgetMeNot-Shaders/commit/4eaa1e0f3bec07f265c504d760cccf2676c8fef5)
         */
        {
            List<String> activeUniforms = new ArrayList<>();
            List<String> unknownUniforms = new ArrayList<>();
            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                IntBuffer size = memoryStack.mallocInt(1);
                IntBuffer type = memoryStack.mallocInt(1);

                int activeUniformsCount = GlStateManager.glGetProgrami(this.getProgramId(), GL33C.GL_ACTIVE_UNIFORMS);
                for (int uniformID = 0; uniformID < activeUniformsCount; uniformID++) {
                    String uniformName = GL20.glGetActiveUniform(this.getProgramId(), uniformID, size, type);
                    activeUniforms.add(uniformName);
                    if (!uniforms.stream().anyMatch(u -> u.name().equals(uniformName)) && !samplers.contains(uniformName)) {
                        unknownUniforms.add(uniformName);
                    }
                }
            }

            for (int i = 0; i < samplers.size(); ++i) {
                var sampler = samplers.get(i);
                if (!activeUniforms.contains(sampler) && unknownUniforms.size() > 0) {
                    String unknownUniform = unknownUniforms.removeFirst();
                    CanPipe.LOGGER.warn("Couldn't find sampler \""+sampler+"\", trying to replace with unknown uniform \""+unknownUniform+"\"");
                    samplers.set(i, unknownUniform);
                }
            }
            this.samplersUniformNames = Collections.unmodifiableList(samplers);
        }

        setupUniforms(uniforms, samplers);

        // accessibility.glsl
        this.FRX_FOV_EFFECTS = getManuallyAppliedUniform("frx_fovEffects");
        this.FRX_DISTORTION_EFFECTS = getManuallyAppliedUniform("frx_distortionEffects");
        this.FRX_HIDE_LIGHTNING_FLASHES = getManuallyAppliedUniform("frx_hideLightningFlashes");
        this.FRX_DARKNESS_PULSING = getManuallyAppliedUniform("frx_darknessPulsing");
        this.FRX_HIGH_CONTRAST = getManuallyAppliedUniform("frx_highContrast");
        this.FRX_DAMAGE_TILT = getManuallyAppliedUniform("frx_damageTilt");
        this.FRX_GLINT_STRENGTH = getManuallyAppliedUniform("frx_glintStrength");
        this.FRX_GLINT_SPEED = getManuallyAppliedUniform("frx_glintSpeed");

        // view.glsl
        this.FRX_MODEL_TO_WORLD = getManuallyAppliedUniform("frx_modelToWorld");
        this.CANPIPE_ORIGIN_TYPE = getUniform("canpipe_originType");  // non-manual
        this.FRX_CAMERA_POS = getManuallyAppliedUniform("frx_cameraPos");
        this.FRX_CAMERA_VIEW = getManuallyAppliedUniform("frx_cameraView");
        this.FRX_LAST_CAMERA_POS = getManuallyAppliedUniform("frx_lastCameraPos");
        this.FRX_INVERSE_VIEW_MATRIX = getManuallyAppliedUniform("frx_inverseViewMatrix");
        this.FRX_LAST_VIEW_MATRIX = getManuallyAppliedUniform("frx_lastViewMatrix");
        this.FRX_INVERSE_PROJECTION_MATRIX = getManuallyAppliedUniform("frx_inverseProjectionMatrix");
        this.FRX_LAST_PROJECTION_MATRIX = getManuallyAppliedUniform("frx_lastProjectionMatrix");
        this.FRX_SHADOW_VIEW_MATRIX = getManuallyAppliedUniform("frx_shadowViewMatrix");
        this.FRX_INVERSE_SHADOW_VIEW_MATRIX = getManuallyAppliedUniform("frx_inverseShadowViewMatrix");
        this.CANPIPE_SHADOW_CENTER_0 = getManuallyAppliedUniform("canpipe_shadowCenter_0");
        this.CANPIPE_SHADOW_CENTER_1 = getManuallyAppliedUniform("canpipe_shadowCenter_1");
        this.CANPIPE_SHADOW_CENTER_2 = getManuallyAppliedUniform("canpipe_shadowCenter_2");
        this.CANPIPE_SHADOW_CENTER_3 = getManuallyAppliedUniform("canpipe_shadowCenter_3");
        this.FRX_VIEW_DISTANCE = getManuallyAppliedUniform("frx_viewDistance");
        this.FRX_VIEW_BRIGHTNESS = getManuallyAppliedUniform("frx_viewBrightness");
        this.CANPIPE_VIEW_FLAGS = getManuallyAppliedUniform("canpipe_viewFlags");
        this.CANPIPE_SCREEN_SIZE = getManuallyAppliedUniform("canpipe_screenSize");

        // player.glsl
        this.FRX_EFFECT_MODIFIER = getManuallyAppliedUniform("frx_effectModifier");
        this.CANPIPE_DARKNESS_FACTOR = getManuallyAppliedUniform("canpipe_darknessFactor");
        this.FRX_EYE_POS = getManuallyAppliedUniform("frx_eyePos");
        this.FRX_EYE_BRIGHTNESS = getManuallyAppliedUniform("frx_eyeBrightness");
        this.FRX_SMOOTHED_EYE_BRIGHTNESS = getManuallyAppliedUniform("frx_smoothedEyeBrightness");
        this.FRX_HELD_LIGHT = getManuallyAppliedUniform("frx_heldLight");
        this.FRX_HELD_LIGHT_INNER_RADIUS = getManuallyAppliedUniform("frx_heldLightInnerRadius");
        this.FRX_HELD_LIGHT_OUTER_RADIUS = getManuallyAppliedUniform("frx_heldLightOuterRadius");
        this.FRX_PLAYER_MOOD = getManuallyAppliedUniform("frx_playerMood");
        this.CANPIPE_PLAYER_FLAGS = getManuallyAppliedUniform("canpipe_playerFlags");
        this.CANPIPE_EFFECTS_FLAGS = getManuallyAppliedUniform("canpipe_effectsFlags");

        // world.glsl
        this.CANPIPE_RENDER_FRAMES = getManuallyAppliedUniform("canpipe_renderFrames");
        this.CANPIPE_FIXED_OR_DAY_TIME = getManuallyAppliedUniform("canpipe_fixedOrDayTime");
        this.FRX_RENDER_SECONDS = getManuallyAppliedUniform("frx_renderSeconds");
        this.FRX_WORLD_DAY = getManuallyAppliedUniform("frx_worldDay");
        this.FRX_WORLD_TIME = getManuallyAppliedUniform("frx_worldTime");
        this.FRX_MOON_SIZE = getManuallyAppliedUniform("frx_moonSize");
        this.FRX_SKY_LIGHT_VECTOR = getManuallyAppliedUniform("frx_skyLightVector");
        this.FRX_SKY_ANGLE_RADIANS = getManuallyAppliedUniform("frx_skyAngleRadians");
        this.CANPIPE_SUNRISE_OR_SUNSET_COLOR = getManuallyAppliedUniform("canpipe_sunriseOrSunsetColor");
        this.FRX_SKY_FLASH_STRENGTH = getManuallyAppliedUniform("frx_skyFlashStrength");
        this.FRX_AMBIENT_INTENSITY = getManuallyAppliedUniform("frx_ambientIntensity");
        this.FRX_EMISSIVE_COLOR = getManuallyAppliedUniform("frx_emissiveColor");
        this.CANPIPE_WORLD_FLAGS = getManuallyAppliedUniform("canpipe_worldFlags");
        this.CANPIPE_WEATHER_GRADIENTS = getManuallyAppliedUniform("canpipe_weatherGradients");

        // fog.glsl
        this.FRX_FOG_COLOR = getManuallyAppliedUniform("frx_fogColor");
        this.FRX_FOG_ENABLED = getManuallyAppliedUniform("frx_fogEnabled");
    }

    private Uniform getManuallyAppliedUniform(String name) {
        Uniform u = this.getUniform(name);
        if (u != null) {
            // it won't be uploaded in GlCommandEncoder.trySetup()
            this.getUniforms().remove(u);
        }
        return u;
    }

    @Override
    public Uniform getUniform(String name) {
        // :evil:, renaming some vanilla uniforms
        if (name.equals("ModelViewMat")) { name = "frx_viewMatrix"; }
        if (name.equals("ProjMat")) { name = "frx_projectionMatrix"; }
        if (name.equals("FogStart")) { name = "frx_fogStart"; }
        if (name.equals("FogEnd")) { name = "frx_fogEnd"; }
        if (name.equals("ModelOffset")) { name = "canpipe_modelToCamera"; }
        return super.getUniform(name);
    }

    @Override
    public void setDefaultUniforms(Mode mode, Matrix4f viewMatrix, Matrix4f projectionMatrix, float w, float h) {
        super.setDefaultUniforms(mode, viewMatrix, projectionMatrix, w, h);
        if (this.CANPIPE_ORIGIN_TYPE != null) {
            this.CANPIPE_ORIGIN_TYPE.set(CanPipe.GlobalState.originType);
        }
    }

    public void setFREXUniforms() {
        Minecraft mc = Minecraft.getInstance();
        Pipeline p = Pipelines.getCurrent();
        GameRendererExtended gre = (GameRendererExtended) mc.gameRenderer;
        LevelRendererExtended lre = (LevelRendererExtended) mc.levelRenderer;
        Camera camera = mc.gameRenderer.getMainCamera();
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 eyePosition = new Vec3(
            Mth.lerp(pt, mc.player.xo, mc.player.getX()),
            Mth.lerp(pt, mc.player.yo, mc.player.getY()) + mc.player.getEyeHeight(),
            Mth.lerp(pt, mc.player.zo, mc.player.getZ())
        );

        GlStateManager._glUseProgram(this.getProgramId());

        if (this.FRX_INVERSE_VIEW_MATRIX != null) {
            this.FRX_INVERSE_VIEW_MATRIX.set(gre.canpipe_getViewMatrix().invert(new Matrix4f()));
            this.FRX_INVERSE_VIEW_MATRIX.upload();
        }
        if (this.FRX_INVERSE_PROJECTION_MATRIX != null) {
            this.FRX_INVERSE_PROJECTION_MATRIX.set(gre.canpipe_getProjectionMatrix().invert(new Matrix4f()));
            this.FRX_INVERSE_PROJECTION_MATRIX.upload();
        }

        // accessibility.glsl
        if (this.FRX_FOV_EFFECTS != null) {
            this.FRX_FOV_EFFECTS.set((float)(double) mc.options.fovEffectScale().get());
            this.FRX_FOV_EFFECTS.upload();
        }
        if (this.FRX_DISTORTION_EFFECTS != null) {
            this.FRX_DISTORTION_EFFECTS.set((float)(double) mc.options.screenEffectScale().get());
            this.FRX_DISTORTION_EFFECTS.upload();
        }
        if (this.FRX_HIDE_LIGHTNING_FLASHES != null) {
            this.FRX_HIDE_LIGHTNING_FLASHES.set(mc.options.hideLightningFlash().get() ? 1 : 0);
            this.FRX_HIDE_LIGHTNING_FLASHES.upload();
        }
        if (this.FRX_DARKNESS_PULSING != null) {
            this.FRX_DARKNESS_PULSING.set((float)(double) mc.options.screenEffectScale().get());
            this.FRX_DARKNESS_PULSING.upload();
        }
        if (this.FRX_HIGH_CONTRAST != null) {
            this.FRX_HIGH_CONTRAST.set(mc.options.highContrast().get() ? 1 : 0);
            this.FRX_HIGH_CONTRAST.upload();
        }
        if (this.FRX_DAMAGE_TILT != null) {
            this.FRX_DAMAGE_TILT.set((float)(double) mc.options.damageTiltStrength().get());
            this.FRX_DAMAGE_TILT.upload();
        }
        if (this.FRX_GLINT_STRENGTH != null) {
            this.FRX_GLINT_STRENGTH.set((float)(double) mc.options.glintStrength().get());
            this.FRX_GLINT_STRENGTH.upload();
        }
        if (this.FRX_GLINT_SPEED != null) {
            this.FRX_GLINT_SPEED.set((float)(double) mc.options.glintSpeed().get());
            this.FRX_GLINT_SPEED.upload();
        }

        // view.glsl
        if (this.FRX_MODEL_TO_WORLD != null) {
            var cameraPos = camera.getPosition();
            this.FRX_MODEL_TO_WORLD.set((float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z, 1.0F);
            this.FRX_MODEL_TO_WORLD.upload();
        }
        if (this.CANPIPE_ORIGIN_TYPE != null) {
            this.CANPIPE_ORIGIN_TYPE.set(0);  // screen
            this.CANPIPE_ORIGIN_TYPE.upload();
        }

        if (this.FRX_CAMERA_POS != null) {
            this.FRX_CAMERA_POS.set(camera.getPosition().toVector3f());
            this.FRX_CAMERA_POS.upload();
        }
        if (this.FRX_CAMERA_VIEW != null) {
            this.FRX_CAMERA_VIEW.set(
                Vec3.directionFromRotation(camera.getXRot(), camera.getYRot()).toVector3f()
            );
            this.FRX_CAMERA_VIEW.upload();
        }
        if (this.FRX_LAST_CAMERA_POS != null) {
            this.FRX_LAST_CAMERA_POS.set(gre.canpipe_getLastCameraPos());
            this.FRX_LAST_CAMERA_POS.upload();
        }
        if (this.FRX_LAST_VIEW_MATRIX != null) {
            this.FRX_LAST_VIEW_MATRIX.set(gre.canpipe_getLastViewMatrix());
            this.FRX_LAST_VIEW_MATRIX.upload();
        }
        if (this.FRX_LAST_PROJECTION_MATRIX != null) {
            this.FRX_LAST_PROJECTION_MATRIX.set(gre.canpipe_getLastProjectionMatrix());
            this.FRX_LAST_PROJECTION_MATRIX.upload();
        }
        if (this.FRX_SHADOW_VIEW_MATRIX != null) {
            this.FRX_SHADOW_VIEW_MATRIX.set(gre.canpipe_getShadowViewMatrix());
            this.FRX_SHADOW_VIEW_MATRIX.upload();
        }
        if (this.FRX_INVERSE_SHADOW_VIEW_MATRIX != null) {
            this.FRX_INVERSE_SHADOW_VIEW_MATRIX.set(gre.canpipe_getShadowViewMatrix().invert(new Matrix4f()));
            this.FRX_INVERSE_SHADOW_VIEW_MATRIX.upload();
        }
        if (this.CANPIPE_SHADOW_CENTER_0 != null) {
            this.CANPIPE_SHADOW_CENTER_0.set(gre.canpipe_getShadowCenters()[0]);
            this.CANPIPE_SHADOW_CENTER_0.upload();
        }
        if (this.CANPIPE_SHADOW_CENTER_1 != null) {
            this.CANPIPE_SHADOW_CENTER_1.set(gre.canpipe_getShadowCenters()[1]);
            this.CANPIPE_SHADOW_CENTER_1.upload();
        }
        if (this.CANPIPE_SHADOW_CENTER_2 != null) {
            this.CANPIPE_SHADOW_CENTER_2.set(gre.canpipe_getShadowCenters()[2]);
            this.CANPIPE_SHADOW_CENTER_2.upload();
        }
        if (this.CANPIPE_SHADOW_CENTER_3 != null) {
            this.CANPIPE_SHADOW_CENTER_3.set(gre.canpipe_getShadowCenters()[3]);
            this.CANPIPE_SHADOW_CENTER_3.upload();
        }
        if (this.FRX_VIEW_DISTANCE != null) {
            this.FRX_VIEW_DISTANCE.set(mc.options.renderDistance().get() * 16.0F);
            this.FRX_VIEW_DISTANCE.upload();
        }
        if (this.FRX_VIEW_BRIGHTNESS != null) {
            this.FRX_VIEW_BRIGHTNESS.set(mc.options.gamma().get().floatValue());
            this.FRX_VIEW_BRIGHTNESS.upload();
        }
        if (this.CANPIPE_VIEW_FLAGS != null) {
            int result = 0;

            BlockPos cameraBlockPos = BlockPos.containing(camera.getPosition());
            Iterable<TagKey<Fluid>> fluidTags = () -> {
                return mc.level.getFluidState(cameraBlockPos).getTags().iterator();
            };
            for (var tag : fluidTags) {
                result |= 1 << 0;  // frx_cameraInFluid
                if (tag.equals(FluidTags.WATER)) {
                    result |= 1 << 1;
                }
                if (tag.equals(FluidTags.LAVA)) {
                    result |= 1 << 2;
                }
            }

            if (mc.level.getBlockState(cameraBlockPos).is(Blocks.POWDER_SNOW)) {
                result |= 1 << 3;
            }

            this.CANPIPE_VIEW_FLAGS.set(result);
            this.CANPIPE_VIEW_FLAGS.upload();
        }
        if (this.CANPIPE_SCREEN_SIZE != null) {
            this.CANPIPE_SCREEN_SIZE.set(
                (float) mc.getWindow().getWidth(),
                (float) mc.getWindow().getHeight()
            );
            this.CANPIPE_SCREEN_SIZE.upload();
        }

        // player.glsl
        if (this.FRX_EFFECT_MODIFIER != null) {
            float effectModifier = 0.0F;
            if (mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
                effectModifier = GameRenderer.getNightVisionScale(mc.player, 0.0F);
            }
            else if (mc.player.hasEffect(MobEffects.CONDUIT_POWER)) {
                effectModifier = mc.player.getWaterVision();
            }
            this.FRX_EFFECT_MODIFIER.set(effectModifier);
            this.FRX_EFFECT_MODIFIER.upload();
        }
        if (this.CANPIPE_DARKNESS_FACTOR != null) {
            float darknessScale = ((LightTextureExtended) mc.gameRenderer.lightTexture()).canpipe_getDarknessScale();
            this.CANPIPE_DARKNESS_FACTOR.set(Mth.clamp(1.0f - darknessScale / 0.45f, 0.0f, 1.0f));
            this.CANPIPE_DARKNESS_FACTOR.upload();
        }
        if (this.FRX_EYE_POS != null) {
            this.FRX_EYE_POS.set(eyePosition.toVector3f());
            this.FRX_EYE_POS.upload();
        }
        if (this.FRX_EYE_BRIGHTNESS != null) {
            this.FRX_EYE_BRIGHTNESS.set(lre.canpipe_getEyeBlockLight(), lre.canpipe_getEyeSkyLight());
            this.FRX_EYE_BRIGHTNESS.upload();
        }
        if (this.FRX_SMOOTHED_EYE_BRIGHTNESS != null) {
            this.FRX_SMOOTHED_EYE_BRIGHTNESS.set(lre.canpipe_getSmoothedEyeBlockLight(), lre.canpipe_getSmoothedEyeSkyLight());
            this.FRX_SMOOTHED_EYE_BRIGHTNESS.upload();
        }

        Light light = ((Supplier<Light>)() -> {
            Item item = mc.player.getMainHandItem().getItem();
            if (item == Items.AIR) item = mc.player.getOffhandItem().getItem();
            if (item == Items.AIR) return null;
            ResourceLocation itemLocation = BuiltInRegistries.ITEM.getKey(item);
            if (itemLocation == null) return null;
            return Lights.get(itemLocation);
        }).get();

        if (this.FRX_HELD_LIGHT != null) {
            if (light != null) {
                this.FRX_HELD_LIGHT.set(light.red, light.green, light.blue, light.intensity);
            }
            else {
                this.FRX_HELD_LIGHT.set(0.0F, 0.0F, 0.0F, 0.0F);
            }
            this.FRX_HELD_LIGHT.upload();
        }
        if (this.FRX_HELD_LIGHT_INNER_RADIUS != null && light != null) {
            this.FRX_HELD_LIGHT_INNER_RADIUS.set(light.innerConeAngle);
            this.FRX_HELD_LIGHT_INNER_RADIUS.upload();
        }
        if (this.FRX_HELD_LIGHT_OUTER_RADIUS != null && light != null) {
            this.FRX_HELD_LIGHT_OUTER_RADIUS.set(light.outerConeAngle);
            this.FRX_HELD_LIGHT_OUTER_RADIUS.upload();
        }
        if (this.FRX_PLAYER_MOOD != null) {
            this.FRX_PLAYER_MOOD.set(mc.player.getCurrentMood());
            this.FRX_PLAYER_MOOD.upload();
        }
        if (this.CANPIPE_PLAYER_FLAGS != null) {
            int result = 0;
            BlockPos bp = BlockPos.containing(eyePosition);
            Iterable<TagKey<Fluid>> fluidTags = ()
                -> mc.level.getFluidState(bp).getTags().iterator();
            for (var tag : fluidTags) {
                result |= 1 << 0;  // frx_playerEyeInFluid
                if (tag.equals(FluidTags.WATER)) {
                    result |= 1 << 1;
                }
                if (tag.equals(FluidTags.LAVA)) {
                    result |= 1 << 2;
                }
            }
            // i have questions about naming of some of them
            result |= (mc.player.isCrouching() ? 1 : 0)                           << 3;
            result |= (mc.player.isSwimming() ? 1 : 0)                            << 4;
            result |= (mc.player.isShiftKeyDown() ? 1 : 0)                        << 5;
            result |= (mc.player.isVisuallySwimming() ? 1 : 0)                    << 6;
            result |= (mc.player.isCreative() ? 1 : 0)                            << 7;
            result |= (mc.player.isSpectator() ? 1 : 0)                           << 8;
            result |= (mc.player.isHandsBusy() ? 1 : 0)                           << 9;
            result |= (mc.player.isOnFire() ? 1 : 0)                              << 10;
            result |= (mc.player.isSleeping() ? 1 : 0)                            << 11;
            result |= (mc.player.isSprinting() ? 1 : 0)                           << 12;
            result |= (mc.player.isInWaterOrRain() ? 1 : 0)                 << 13;
            result |= (mc.level.getBlockState(bp).is(Blocks.POWDER_SNOW) ? 1 : 0) << 14;
            result |= (mc.player.isFreezing() ? 1 : 0)                            << 15;
            this.CANPIPE_PLAYER_FLAGS.set(result);
            this.CANPIPE_PLAYER_FLAGS.upload();
        }
        if (this.CANPIPE_EFFECTS_FLAGS != null) {
            long result = 0;
            result |= (mc.player.hasEffect(MobEffects.SPEED) ? 1L : 0L)      << 0;
            result |= (mc.player.hasEffect(MobEffects.SLOWNESS) ? 1L : 0L)   << 1;
            result |= (mc.player.hasEffect(MobEffects.HASTE) ? 1L : 0L)           << 2;
            result |= (mc.player.hasEffect(MobEffects.MINING_FATIGUE) ? 1L : 0L)        << 3;
            result |= (mc.player.hasEffect(MobEffects.STRENGTH) ? 1L : 0L)        << 4;
            result |= (mc.player.hasEffect(MobEffects.INSTANT_HEALTH) ? 1L : 0L)                << 5;
            result |= (mc.player.hasEffect(MobEffects.INSTANT_DAMAGE) ? 1L : 0L)                << 6;
            result |= (mc.player.hasEffect(MobEffects.JUMP_BOOST) ? 1L : 0L)                << 7;
            result |= (mc.player.hasEffect(MobEffects.NAUSEA) ? 1L : 0L)           << 8;
            result |= (mc.player.hasEffect(MobEffects.REGENERATION) ? 1L : 0L)        << 9;
            result |= (mc.player.hasEffect(MobEffects.RESISTANCE) ? 1L : 0L)   << 10;
            result |= (mc.player.hasEffect(MobEffects.FIRE_RESISTANCE) ? 1L : 0L)     << 11;
            result |= (mc.player.hasEffect(MobEffects.WATER_BREATHING) ? 1L : 0L)     << 12;
            result |= (mc.player.hasEffect(MobEffects.INVISIBILITY) ? 1L : 0L)        << 13;
            result |= (mc.player.hasEffect(MobEffects.BLINDNESS) ? 1L : 0L)           << 14;
            result |= (mc.player.hasEffect(MobEffects.NIGHT_VISION) ? 1L : 0L)        << 15;
            result |= (mc.player.hasEffect(MobEffects.HUNGER) ? 1L : 0L)              << 16;
            result |= (mc.player.hasEffect(MobEffects.WEAKNESS) ? 1L : 0L)            << 17;
            result |= (mc.player.hasEffect(MobEffects.POISON) ? 1L : 0L)              << 18;
            result |= (mc.player.hasEffect(MobEffects.WITHER) ? 1L : 0L)              << 19;
            result |= (mc.player.hasEffect(MobEffects.HEALTH_BOOST) ? 1L : 0L)        << 20;
            result |= (mc.player.hasEffect(MobEffects.ABSORPTION) ? 1L : 0L)          << 21;
            result |= (mc.player.hasEffect(MobEffects.SATURATION) ? 1L : 0L)          << 22;
            result |= (mc.player.hasEffect(MobEffects.GLOWING) ? 1L : 0L)             << 23;
            result |= (mc.player.hasEffect(MobEffects.LEVITATION) ? 1L : 0L)          << 24;
            result |= (mc.player.hasEffect(MobEffects.LUCK) ? 1L : 0L)                << 25;
            result |= (mc.player.hasEffect(MobEffects.UNLUCK) ? 1L : 0L)              << 26;
            result |= (mc.player.hasEffect(MobEffects.SLOW_FALLING) ? 1L : 0L)        << 27;
            result |= (mc.player.hasEffect(MobEffects.CONDUIT_POWER) ? 1L : 0L)       << 28;
            result |= (mc.player.hasEffect(MobEffects.DOLPHINS_GRACE) ? 1L : 0L)      << 29;
            result |= (mc.player.hasEffect(MobEffects.BAD_OMEN) ? 1L : 0L)            << 30;
            result |= (mc.player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) ? 1L : 0L) << 31;
            result |= (mc.player.hasEffect(MobEffects.DARKNESS) ? 1L : 0L)            << 32;
            this.CANPIPE_EFFECTS_FLAGS.set((int)(result & 0xFFFFFFFFL), (int)(result >>> 32));
            this.CANPIPE_EFFECTS_FLAGS.upload();
        }

        // world
        if (this.CANPIPE_RENDER_FRAMES != null) {
            this.CANPIPE_RENDER_FRAMES.set(gre.canpipe_getFrame());
            this.CANPIPE_RENDER_FRAMES.upload();
        }
        if (this.FRX_RENDER_SECONDS != null) {
            this.FRX_RENDER_SECONDS.set(gre.canpipe_getRenderSeconds());
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
        if (this.FRX_MOON_SIZE != null) {
            this.FRX_MOON_SIZE.set(mc.level.getMoonBrightness());
            this.FRX_MOON_SIZE.upload();
        }
        if (this.FRX_SKY_LIGHT_VECTOR != null) {
            this.FRX_SKY_LIGHT_VECTOR.set(p.getSunOrMoonDir(mc.level, new Vector3f(), pt));
            this.FRX_SKY_LIGHT_VECTOR.upload();
        }
        if (this.FRX_SKY_ANGLE_RADIANS != null) {
            this.FRX_SKY_ANGLE_RADIANS.set(mc.level.getSunAngle(pt));
            this.FRX_SKY_ANGLE_RADIANS.upload();
        }
        if (this.CANPIPE_SUNRISE_OR_SUNSET_COLOR != null) {
            var timeOfDay = mc.level.getTimeOfDay(pt);
            var result = new Vector3f(1.0F);
            if (
                mc.level.dimensionType().hasSkyLight()
                && mc.level.effects().isSunriseOrSunset(timeOfDay)
            ) {
                int color = mc.level.effects().getSunriseOrSunsetColor(timeOfDay);
                result.set((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF);
                result.div(255.0F);
            }
            this.CANPIPE_SUNRISE_OR_SUNSET_COLOR.set(result);
            this.CANPIPE_SUNRISE_OR_SUNSET_COLOR.upload();
        }
        if (this.FRX_SKY_FLASH_STRENGTH != null) {
            float skyFlashStrength = Math.max(0.0F, mc.level.getSkyFlashTime()-pt);
            this.FRX_SKY_FLASH_STRENGTH.set(skyFlashStrength);
            this.FRX_SKY_FLASH_STRENGTH.upload();
        }
        if (this.FRX_AMBIENT_INTENSITY != null) {
            // Not sure why partial tick is 1.0 (LightTexture.updateLigthTexture)
            this.FRX_AMBIENT_INTENSITY.set(mc.level.getSkyDarken(1.0F));
            this.FRX_AMBIENT_INTENSITY.upload();
        }
        if (this.FRX_EMISSIVE_COLOR != null) {
            Vector4f emissiveColor = (
                (LightTextureExtended) mc.gameRenderer.lightTexture()
            ).canpipe_getEmissiveColor();
            this.FRX_EMISSIVE_COLOR.set(emissiveColor);
            this.FRX_EMISSIVE_COLOR.upload();
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
                mc.level.getRainLevel(pt),
                mc.level.getThunderLevel(pt),
                lre.canpipe_getSmoothedRainGradient(),
                lre.canpipe_getSmoothedThunderGradient()
            );
            this.CANPIPE_WEATHER_GRADIENTS.upload();
        }

        // fog.glsl
        if (this.FRX_FOG_COLOR != null) {
            this.FRX_FOG_COLOR.set(FogRenderer.computeFogColor(
                mc.gameRenderer.getMainCamera(),
                pt,
                mc.level, mc.options.getEffectiveRenderDistance(),
                mc.gameRenderer.getDarkenWorldAmount(pt)
            ));
            this.FRX_FOG_COLOR.upload();
        }

        GlStateManager._glUseProgram(0);
    }

    protected boolean samplerExists(String sampler) {
        for (int i = 0; i < this.getSamplers().size(); ++i) {
            if (this.getSamplers().get(i).equals(sampler)) {
                return true;
            }
        }
        return false;
    }

}
