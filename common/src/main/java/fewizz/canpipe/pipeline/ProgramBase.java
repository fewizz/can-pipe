package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlShaderModule;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.UniformBuffer;
import fewizz.canpipe.UniformBuffer.FloatUniform;
import fewizz.canpipe.UniformBuffer.IVec2Uniform;
import fewizz.canpipe.UniformBuffer.IntUniform;
import fewizz.canpipe.UniformBuffer.Mat4Uniform;
import fewizz.canpipe.UniformBuffer.Vec2Uniform;
import fewizz.canpipe.UniformBuffer.Vec3Uniform;
import fewizz.canpipe.UniformBuffer.Vec4Uniform;
import fewizz.canpipe.light.Light;
import fewizz.canpipe.light.Lights;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.mixininterface.LightTextureExtended;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderManager.CompilationException;
import net.minecraft.client.renderer.fog.FogRenderer;
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

    // accessibility
    private static final UniformBuffer ACCESSIBILITY = new UniformBuffer();
    private static final FloatUniform FRX_FOV_EFFECTS = ACCESSIBILITY.add(new FloatUniform());
    private static final FloatUniform FRX_DISTORTION_EFFECTS = ACCESSIBILITY.add(new FloatUniform());
    private static final FloatUniform FRX_DARKNESS_PULSING = ACCESSIBILITY.add(new FloatUniform());
    private static final FloatUniform FRX_DAMAGE_TILT = ACCESSIBILITY.add(new FloatUniform());
    private static final FloatUniform FRX_GLINT_STRENGTH = ACCESSIBILITY.add(new FloatUniform());
    private static final FloatUniform FRX_GLINT_SPEED = ACCESSIBILITY.add(new FloatUniform());
    private static final IntUniform FRX_HIDE_LIGHTNING_FLASHES = ACCESSIBILITY.add(new IntUniform());
    private static final IntUniform FRX_HIGH_CONTRAST = ACCESSIBILITY.add(new IntUniform());
    public static final GpuBuffer ACCESSIBILITY_UBO = RenderSystem.getDevice().createBuffer(
        () -> "can-pipe accessibility UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, ACCESSIBILITY.size()
    );

    // view
    private static final UniformBuffer VIEW = new UniformBuffer();
    private static final Mat4Uniform FRX_INVERSE_VIEW_MATRIX = VIEW.add(new Mat4Uniform());
    private static final Mat4Uniform FRX_LAST_VIEW_MATRIX = VIEW.add(new Mat4Uniform());
    private static final Mat4Uniform FRX_INVERSE_PROJECTION_MATRIX = VIEW.add(new Mat4Uniform());
    private static final Mat4Uniform FRX_LAST_PROJECTION_MATRIX = VIEW.add(new Mat4Uniform());
    private static final Mat4Uniform FRX_SHADOW_VIEW_MATRIX = VIEW.add(new Mat4Uniform());
    private static final Mat4Uniform FRX_INVERSE_SHADOW_VIEW_MATRIX = VIEW.add(new Mat4Uniform());
    private static final Vec4Uniform FRX_MODEL_TO_WORLD = VIEW.add(new Vec4Uniform());
    private static final Vec4Uniform CANPIPE_SHADOW_CENTER_0 = VIEW.add(new Vec4Uniform());
    private static final Vec4Uniform CANPIPE_SHADOW_CENTER_1 = VIEW.add(new Vec4Uniform());
    private static final Vec4Uniform CANPIPE_SHADOW_CENTER_2 = VIEW.add(new Vec4Uniform());
    private static final Vec4Uniform CANPIPE_SHADOW_CENTER_3 = VIEW.add(new Vec4Uniform());
    private static final Vec2Uniform CANPIPE_SCREEN_SIZE = VIEW.add(new Vec2Uniform());
    private static final FloatUniform FRX_VIEW_BRIGHTNESS = VIEW.add(new FloatUniform());
    private static final FloatUniform FRX_VIEW_DISTANCE = VIEW.add(new FloatUniform());
    private static final IntUniform CANPIPE_VIEW_FLAGS = VIEW.add(new IntUniform());
    private static final Vec3Uniform FRX_CAMERA_VIEW = VIEW.add(new Vec3Uniform());
    private static final Vec3Uniform FRX_CAMERA_POS = VIEW.add(new Vec3Uniform());
    private static final Vec3Uniform FRX_LAST_CAMERA_POS = VIEW.add(new Vec3Uniform());
    public static final GpuBuffer VIEW_UBO = RenderSystem.getDevice().createBuffer(
        () -> "can-pipe view UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, VIEW.size()
    );

    // player
    private static final UniformBuffer PLAYER = new UniformBuffer();
    private static final FloatUniform FRX_EFFECT_MODIFIER = PLAYER.add(new FloatUniform());
    private static final FloatUniform CANPIPE_DARKNESS_FACTOR = PLAYER.add(new FloatUniform());
    private static final Vec3Uniform FRX_EYE_POS = PLAYER.add(new Vec3Uniform());
    private static final Vec2Uniform FRX_EYE_BRIGHTNESS = PLAYER.add(new Vec2Uniform());
    private static final Vec2Uniform FRX_SMOOTHED_EYE_BRIGHTNESS = PLAYER.add(new Vec2Uniform());
    private static final Vec4Uniform FRX_HELD_LIGHT = PLAYER.add(new Vec4Uniform());
    private static final FloatUniform FRX_HELD_LIGHT_INNER_RADIUS = PLAYER.add(new FloatUniform());
    private static final FloatUniform FRX_HELD_LIGHT_OUTER_RADIUS = PLAYER.add(new FloatUniform());
    private static final FloatUniform FRX_PLAYER_MOOD = PLAYER.add(new FloatUniform());
    private static final IntUniform CANPIPE_PLAYER_FLAGS = PLAYER.add(new IntUniform());
    private static final IVec2Uniform CANPIPE_EFFECTS_FLAGS = PLAYER.add(new IVec2Uniform());
    public static final GpuBuffer PLAYER_UBO = RenderSystem.getDevice().createBuffer(
        () -> "can-pipe player UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, PLAYER.size()
    );

    // world
    private static final UniformBuffer WORLD = new UniformBuffer();
    private static final IntUniform CANPIPE_RENDER_FRAMES = WORLD.add(new IntUniform());
    private static final IntUniform CANPIPE_WORLD_FLAGS = WORLD.add(new IntUniform());
    private static final FloatUniform CANPIPE_FIXED_OR_DAY_TIME = WORLD.add(new FloatUniform());
    private static final FloatUniform FRX_RENDER_SECONDS = WORLD.add(new FloatUniform());
    private static final FloatUniform FRX_WORLD_DAY = WORLD.add(new FloatUniform());
    private static final FloatUniform FRX_WORLD_TIME = WORLD.add(new FloatUniform());
    private static final FloatUniform FRX_MOON_SIZE = WORLD.add(new FloatUniform());
    private static final FloatUniform FRX_SKY_ANGLE_RADIANS = WORLD.add(new FloatUniform());
    private static final FloatUniform FRX_SKY_FLASH_STRENGTH = WORLD.add(new FloatUniform());
    private static final FloatUniform FRX_AMBIENT_INTENSITY = WORLD.add(new FloatUniform());
    private static final Vec4Uniform FRX_EMISSIVE_COLOR = WORLD.add(new Vec4Uniform());
    private static final Vec4Uniform CANPIPE_WEATHER_GRADIENTS = WORLD.add(new Vec4Uniform());
    private static final Vec3Uniform FRX_SKY_LIGHT_VECTOR = WORLD.add(new Vec3Uniform());
    private static final Vec3Uniform CANPIPE_SUNRISE_OR_SUNSET_COLOR = WORLD.add(new Vec3Uniform());
    public static final GpuBuffer WORLD_UBO = RenderSystem.getDevice().createBuffer(
        () -> "can-pipe world UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, WORLD.size()
    );

    // fog
    private static final UniformBuffer FOG = new UniformBuffer();
    private static final Vec4Uniform FRX_FOG_COLOR = FOG.add(new Vec4Uniform() {{ value.set(1.0F); }});
    private static final IntUniform FRX_FOG_ENABLED = FOG.add(new IntUniform());
    public static final GpuBuffer FOG_UBO = RenderSystem.getDevice().createBuffer(
        () -> "can-pipe fog UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, FOG.size()
    );

    public final Shader vertexShader;
    public final Shader fragmentShader;

    private static final List<RenderPipeline.UniformDescription> DEFAULT_UNIFORMS = List.of(
        // accessibility.glsl
        new RenderPipeline.UniformDescription("frx_ub_accessibility", UniformType.UNIFORM_BUFFER),

        // view.glsl
        new RenderPipeline.UniformDescription("frx_ub_view", UniformType.UNIFORM_BUFFER),

        // player.glsl
        new RenderPipeline.UniformDescription("frx_ub_player", UniformType.UNIFORM_BUFFER),

        // world.glsl
        new RenderPipeline.UniformDescription("frx_ub_world", UniformType.UNIFORM_BUFFER),

        // fog.glsl
        new RenderPipeline.UniformDescription("frx_ub_fog", UniformType.UNIFORM_BUFFER),

        // mc
        new RenderPipeline.UniformDescription("mc_ub_dynamic_transforms", UniformType.UNIFORM_BUFFER),
        new RenderPipeline.UniformDescription("mc_ub_projection", UniformType.UNIFORM_BUFFER),
        new RenderPipeline.UniformDescription("mc_ub_fog", UniformType.UNIFORM_BUFFER)
    );

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
         * - Aerie v1.0.0 "copy" program
         *   (https://github.com/ambrosia13/Aerie-Shaders/pull/2),
         * - Forget-me-not v0.8.0 "depth_downsample" program
         *   (https://github.com/ambrosia13/ForgetMeNot-Shaders/commit/4eaa1e0f3bec07f265c504d760cccf2676c8fef5)
         */
        /*{
            List<String> activeUniforms = new ArrayList<>();
            List<String> unknownUniforms = new ArrayList<>();
            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                IntBuffer size = memoryStack.mallocInt(1);
                IntBuffer type = memoryStack.mallocInt(1);

                int activeUniformsCount = GlStateManager.glGetProgrami(this.getProgramId(), GL33C.GL_ACTIVE_UNIFORMS);
                for (int uniformID = 0; uniformID < activeUniformsCount; uniformID++) {
                    String uniformName = GFX.glGetActiveUniform(this.getProgramId(), uniformID, size, type);
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
        }*/

        this.setupUniforms(uniforms, samplers);

        // Some dirty aliasing hacks
        var dynamicTransformsUB = getUniforms().remove("mc_ub_dynamic_transforms");
        if (dynamicTransformsUB != null) {
            getUniforms().put("DynamicTransforms", dynamicTransformsUB);
        }

        var projectionUB = getUniforms().remove("mc_ub_projection");
        if (projectionUB != null) {
            getUniforms().put("Projection", projectionUB);
        }

        var fogUB = getUniforms().remove("mc_ub_fog");
        if (fogUB != null) {
            getUniforms().put("Fog", fogUB);
        }
    }

    public static void updateFREXUniforms() {
        Minecraft mc = Minecraft.getInstance();
        Pipeline p = Pipelines.getCurrent();
        GameRendererExtended gre = (GameRendererExtended) mc.gameRenderer;
        LevelRendererExtended lre = (LevelRendererExtended) mc.levelRenderer;
        Camera camera = mc.gameRenderer.getMainCamera();
        var cameraPos = camera.getPosition();
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 eyePosition = new Vec3(
            Mth.lerp(pt, mc.player.xo, mc.player.getX()),
            Mth.lerp(pt, mc.player.yo, mc.player.getY()) + mc.player.getEyeHeight(),
            Mth.lerp(pt, mc.player.zo, mc.player.getZ())
        );

        FRX_INVERSE_VIEW_MATRIX.value.set(gre.canpipe_getViewMatrix().invert(new Matrix4f()));
        FRX_INVERSE_PROJECTION_MATRIX.value.set(gre.canpipe_getProjectionMatrix().invert(new Matrix4f()));

        // accessibility.glsl
        FRX_FOV_EFFECTS.value = (float)(double) mc.options.fovEffectScale().get();
        FRX_DISTORTION_EFFECTS.value = (float)(double) mc.options.screenEffectScale().get();
        FRX_HIDE_LIGHTNING_FLASHES.value = mc.options.hideLightningFlash().get() ? 1 : 0;
        FRX_DARKNESS_PULSING.value = (float)(double) mc.options.screenEffectScale().get();
        FRX_HIGH_CONTRAST.value = mc.options.highContrast().get() ? 1 : 0;
        FRX_DAMAGE_TILT.value = (float)(double) mc.options.damageTiltStrength().get();
        FRX_GLINT_STRENGTH.value = (float)(double) mc.options.glintStrength().get();
        FRX_GLINT_SPEED.value = (float)(double) mc.options.glintSpeed().get();
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            var builder = Std140Builder.onStack(memoryStack, ACCESSIBILITY.size());
            ACCESSIBILITY.writeTo(builder);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(ACCESSIBILITY_UBO.slice(), builder.get());
        }

        // view.glsl
        FRX_MODEL_TO_WORLD.value.set((float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z, 1.0F);
        FRX_CAMERA_POS.value.set(cameraPos.toVector3f());
        FRX_CAMERA_VIEW.value.set(
            Vec3.directionFromRotation(camera.getXRot(), camera.getYRot()).toVector3f()
        );
        FRX_LAST_CAMERA_POS.value.set(gre.canpipe_getLastCameraPos());
        FRX_LAST_VIEW_MATRIX.value.set(gre.canpipe_getLastViewMatrix());
        FRX_LAST_PROJECTION_MATRIX.value.set(gre.canpipe_getLastProjectionMatrix());
        FRX_SHADOW_VIEW_MATRIX.value.set(gre.canpipe_getShadowViewMatrix()); 
        FRX_INVERSE_SHADOW_VIEW_MATRIX.value.set(gre.canpipe_getShadowViewMatrix().invert(new Matrix4f()));
        CANPIPE_SHADOW_CENTER_0.value.set(gre.canpipe_getShadowCenters()[0]);
        CANPIPE_SHADOW_CENTER_1.value.set(gre.canpipe_getShadowCenters()[1]);
        CANPIPE_SHADOW_CENTER_2.value.set(gre.canpipe_getShadowCenters()[2]);
        CANPIPE_SHADOW_CENTER_3.value.set(gre.canpipe_getShadowCenters()[3]);
        FRX_VIEW_DISTANCE.value = mc.options.renderDistance().get() * 16.0F;
        FRX_VIEW_BRIGHTNESS.value = mc.options.gamma().get().floatValue();
        {
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

            CANPIPE_VIEW_FLAGS.value = result;
        }
        CANPIPE_SCREEN_SIZE.value.set(
            (float) mc.getWindow().getWidth(),
            (float) mc.getWindow().getHeight()
        );

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            var builder = Std140Builder.onStack(memoryStack, VIEW.size());
            VIEW.writeTo(builder);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(VIEW_UBO.slice(), builder.get());
        }

        // player.glsl
        {
            float effectModifier = 0.0F;
            if (mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
                effectModifier = GameRenderer.getNightVisionScale(mc.player, 0.0F);
            }
            else if (mc.player.hasEffect(MobEffects.CONDUIT_POWER)) {
                effectModifier = mc.player.getWaterVision();
            }
            FRX_EFFECT_MODIFIER.value = effectModifier;
        }
        {
            float darknessScale = ((LightTextureExtended) mc.gameRenderer.lightTexture()).canpipe_getDarknessScale();
            CANPIPE_DARKNESS_FACTOR.value = Mth.clamp(1.0f - darknessScale / 0.45f, 0.0f, 1.0f);
        }
        FRX_EYE_POS.value.set(eyePosition.toVector3f());
        FRX_EYE_BRIGHTNESS.value.set(lre.canpipe_getEyeBlockLight(), lre.canpipe_getEyeSkyLight());
        FRX_SMOOTHED_EYE_BRIGHTNESS.value.set(lre.canpipe_getSmoothedEyeBlockLight(), lre.canpipe_getSmoothedEyeSkyLight());

        Light light = ((Supplier<Light>)() -> {
            Item item = mc.player.getMainHandItem().getItem();
            if (item == Items.AIR) item = mc.player.getOffhandItem().getItem();
            if (item == Items.AIR) return null;
            ResourceLocation itemLocation = BuiltInRegistries.ITEM.getKey(item);
            if (itemLocation == null) return null;
            return Lights.get(itemLocation);
        }).get();

        {
            if (light != null) {
                FRX_HELD_LIGHT.value.set(light.red, light.green, light.blue, light.intensity);
                FRX_HELD_LIGHT_INNER_RADIUS.value = light.innerConeAngle;
                FRX_HELD_LIGHT_OUTER_RADIUS.value = light.outerConeAngle;
            }
            else {
                FRX_HELD_LIGHT.value.set(0.0F);
            }
        }
        FRX_PLAYER_MOOD.value = mc.player.getCurrentMood();
        {
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
            CANPIPE_PLAYER_FLAGS.value = result;
        }
        {
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
            CANPIPE_EFFECTS_FLAGS.value.set((int)(result & 0xFFFFFFFFL), (int)(result >>> 32));
        }

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            var builder = Std140Builder.onStack(memoryStack, PLAYER.size());
            PLAYER.writeTo(builder);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(PLAYER_UBO.slice(), builder.get());
        }

        // world
        CANPIPE_RENDER_FRAMES.value = gre.canpipe_getFrame();
        FRX_RENDER_SECONDS.value = gre.canpipe_getRenderSeconds();
        {
            long ticks = mc.level.dimensionType().fixedTime().orElse(mc.level.getDayTime());
            CANPIPE_FIXED_OR_DAY_TIME.value = (ticks % 24000L) / 24000.0F;
        }
        FRX_WORLD_DAY.value = mc.level != null ? (mc.level.getDayTime() / 24000L) % 2147483647L : 0.0F;
        FRX_WORLD_TIME.value = mc.level != null ? (mc.level.getDayTime() % 24000L) / 24000.0F : 0.0F;
        FRX_MOON_SIZE.value = mc.level.getMoonBrightness();
        FRX_SKY_LIGHT_VECTOR.value.set(p.getSunOrMoonDir(mc.level, new Vector3f(), pt));
        FRX_SKY_ANGLE_RADIANS.value = mc.level.getSunAngle(pt);
        {
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
            CANPIPE_SUNRISE_OR_SUNSET_COLOR.value.set(result);
        }
        {
            float skyFlashStrength = Math.max(0.0F, mc.level.getSkyFlashTime()-pt);
            FRX_SKY_FLASH_STRENGTH.value = skyFlashStrength;
        }
        // Not sure why partial tick is 1.0 (LightTexture.updateLigthTexture)
        FRX_AMBIENT_INTENSITY.value = mc.level.getSkyDarken(1.0F);
        {
            Vector4f emissiveColor = (
                (LightTextureExtended) mc.gameRenderer.lightTexture()
            ).canpipe_getEmissiveColor();
            FRX_EMISSIVE_COLOR.value.set(emissiveColor);
        }
        {
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

            CANPIPE_WORLD_FLAGS.value = value;
        }
        CANPIPE_WEATHER_GRADIENTS.value.set(
            mc.level.getRainLevel(pt),
            mc.level.getThunderLevel(pt),
            lre.canpipe_getSmoothedRainGradient(),
            lre.canpipe_getSmoothedThunderGradient()
        );

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            var builder = Std140Builder.onStack(memoryStack, WORLD.size());
            WORLD.writeTo(builder);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(WORLD_UBO.slice(), builder.get());
        }

        // fog.glsl
        FRX_FOG_COLOR.value.set(
            gre.canpipe_getFogRenderer().setupFog(
                mc.gameRenderer.getMainCamera(),
                mc.options.getEffectiveRenderDistance(),
                false,
                mc.getDeltaTracker(),
                mc.gameRenderer.getDarkenWorldAmount(pt),
                mc.level
            )
        );
        FRX_FOG_ENABLED.value = 1;  // TODO?

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            var builder = Std140Builder.onStack(memoryStack, FOG.size());
            FOG.writeTo(builder);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(FOG_UBO.slice(), builder.get());
        }
    }

}
