package fewizz.canpipe;

import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;

import fewizz.canpipe.UniformBufferStruct.FloatUniform;
import fewizz.canpipe.UniformBufferStruct.IVec2Uniform;
import fewizz.canpipe.UniformBufferStruct.IntUniform;
import fewizz.canpipe.UniformBufferStruct.Mat4Uniform;
import fewizz.canpipe.UniformBufferStruct.Vec2Uniform;
import fewizz.canpipe.UniformBufferStruct.Vec3Uniform;
import fewizz.canpipe.UniformBufferStruct.Vec4Uniform;
import fewizz.canpipe.light.Light;
import fewizz.canpipe.light.Lights;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

public class Uniforms {

    public static final GpuBuffer[] INT_0_4_UBO_BUFFERS = new GpuBuffer[] {
        RenderSystem.getDevice().createBuffer(
            () -> "can-pipe 0",
            GpuBuffer.USAGE_UNIFORM,
            MemoryUtil.memByteBuffer(MemoryUtil.memAllocInt(1).put(0, 0))
        ),
        RenderSystem.getDevice().createBuffer(
            () -> "can-pipe 1",
            GpuBuffer.USAGE_UNIFORM,
            MemoryUtil.memByteBuffer(MemoryUtil.memAllocInt(1).put(0, 1))
        ),
        RenderSystem.getDevice().createBuffer(
            () -> "can-pipe 2",
            GpuBuffer.USAGE_UNIFORM,
            MemoryUtil.memByteBuffer(MemoryUtil.memAllocInt(1).put(0, 2))
        ),
        RenderSystem.getDevice().createBuffer(
            () -> "can-pipe 3",
            GpuBuffer.USAGE_UNIFORM,
            MemoryUtil.memByteBuffer(MemoryUtil.memAllocInt(1).put(0, 3))
        )
    };

    // accessibility
    private static final UniformBufferStruct ACCESSIBILITY = new UniformBufferStruct();
    private static final FloatUniform FRX_FOV_EFFECTS = ACCESSIBILITY.add(new FloatUniform());
    private static final FloatUniform FRX_DISTORTION_EFFECTS = ACCESSIBILITY.add(new FloatUniform());
    private static final FloatUniform FRX_DARKNESS_PULSING = ACCESSIBILITY.add(new FloatUniform());
    private static final FloatUniform FRX_DAMAGE_TILT = ACCESSIBILITY.add(new FloatUniform());
    private static final FloatUniform FRX_GLINT_STRENGTH = ACCESSIBILITY.add(new FloatUniform());
    private static final FloatUniform FRX_GLINT_SPEED = ACCESSIBILITY.add(new FloatUniform());
    private static final IntUniform FRX_HIDE_LIGHTNING_FLASHES = ACCESSIBILITY.add(new IntUniform());
    private static final IntUniform FRX_HIGH_CONTRAST = ACCESSIBILITY.add(new IntUniform());
    public static final GpuBuffer ACCESSIBILITY_UBO = RenderSystem.getDevice().createBuffer(
        () -> "can-pipe accessibility UBO",
        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
        ACCESSIBILITY.size()
    );

    // view
    private static final UniformBufferStruct VIEW = new UniformBufferStruct();
    public static final Mat4Uniform FRX_INVERSE_VIEW_MATRIX = VIEW.add(new Mat4Uniform());
    public static final Mat4Uniform FRX_LAST_VIEW_MATRIX = VIEW.add(new Mat4Uniform());
    public static final Mat4Uniform FRX_INVERSE_PROJECTION_MATRIX = VIEW.add(new Mat4Uniform());
    public static final Mat4Uniform FRX_LAST_PROJECTION_MATRIX = VIEW.add(new Mat4Uniform());
    private static final Vec4Uniform FRX_MODEL_TO_WORLD = VIEW.add(new Vec4Uniform());
    private static final Vec2Uniform CANPIPE_SCREEN_SIZE = VIEW.add(new Vec2Uniform());
    private static final FloatUniform FRX_VIEW_BRIGHTNESS = VIEW.add(new FloatUniform());
    private static final FloatUniform FRX_VIEW_DISTANCE = VIEW.add(new FloatUniform());
    private static final IntUniform CANPIPE_VIEW_FLAGS = VIEW.add(new IntUniform());
    private static final Vec3Uniform FRX_CAMERA_VIEW = VIEW.add(new Vec3Uniform());
    public static final Vec3Uniform FRX_CAMERA_POS = VIEW.add(new Vec3Uniform());
    public static final Vec3Uniform FRX_LAST_CAMERA_POS = VIEW.add(new Vec3Uniform());
    public static final GpuBuffer VIEW_UBO = RenderSystem.getDevice().createBuffer(
        () -> "can-pipe view UBO",
        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
        VIEW.size()
    );

    private static final UniformBufferStruct SHADOW = new UniformBufferStruct();
    public static final Mat4Uniform FRX_SHADOW_VIEW_MATRIX = SHADOW.add(new Mat4Uniform());
    public static final Mat4Uniform FRX_INVERSE_SHADOW_VIEW_MATRIX = SHADOW.add(new Mat4Uniform());
    public static final Vec4Uniform[] CANPIPE_SHADOW_CENTERS = new Vec4Uniform[] {
        SHADOW.add(new Vec4Uniform()),
        SHADOW.add(new Vec4Uniform()),
        SHADOW.add(new Vec4Uniform()),
        SHADOW.add(new Vec4Uniform())
    };
    public static final GpuBuffer SHADOW_UBO = RenderSystem.getDevice().createBuffer(
        () -> "can-pipe shadow UBO",
        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
        SHADOW.size()
    );


    public static final GpuBuffer PASS_DYNAMIC_TRANSFORMS_UBO = RenderSystem.getDevice().createBuffer(
        () -> "can-pipe pass dynamic transforms UBO",
        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
        DynamicUniforms.TRANSFORM_UBO_SIZE
    );

    // player
    private static final UniformBufferStruct PLAYER = new UniformBufferStruct();
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
        () -> "can-pipe player UBO",
        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
        PLAYER.size()
    );

    // world
    private static final UniformBufferStruct WORLD = new UniformBufferStruct();
    public static final IntUniform CANPIPE_RENDER_FRAMES = WORLD.add(new IntUniform());
    private static final IntUniform CANPIPE_WORLD_FLAGS = WORLD.add(new IntUniform());
    private static final FloatUniform CANPIPE_FIXED_OR_DAY_TIME = WORLD.add(new FloatUniform());
    public static final FloatUniform FRX_RENDER_SECONDS = WORLD.add(new FloatUniform());
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
        () -> "can-pipe world UBO",
        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
        WORLD.size()
    );

    // fog
    private static final UniformBufferStruct FOG = new UniformBufferStruct();
    private static final Vec4Uniform FRX_FOG_COLOR = FOG.add(new Vec4Uniform() {{ set(1.0F); }});
    private static final IntUniform FRX_FOG_ENABLED = FOG.add(new IntUniform());
    public static final GpuBuffer FOG_UBO = RenderSystem.getDevice().createBuffer(
        () -> "can-pipe fog UBO",
        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
        FOG.size()
    );

    public static void updateFREXUniforms(Matrix4f view, Matrix4f projection) {
        Profiler.get().push("can-pipe update FREX uniforms");
        Profiler.get().push("collect");

        Minecraft mc = Minecraft.getInstance();
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        Pipeline p = Pipelines.getCurrent();
        GameRendererExtended gre = (GameRendererExtended) mc.gameRenderer;
        LevelRendererExtended lre = (LevelRendererExtended) mc.levelRenderer;
        Camera camera = mc.gameRenderer.getMainCamera();
        var cameraPos = camera.position();
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 eyePosition = new Vec3(
            Mth.lerp(pt, mc.player.xo, mc.player.getX()),
            Mth.lerp(pt, mc.player.yo, mc.player.getY()) + mc.player.getEyeHeight(),
            Mth.lerp(pt, mc.player.zo, mc.player.getZ())
        );

        FRX_INVERSE_VIEW_MATRIX.set(view).invert();
        FRX_INVERSE_PROJECTION_MATRIX.set(projection).invert();

        // accessibility.glsl
        FRX_FOV_EFFECTS.set((float)(double) mc.options.fovEffectScale().get());
        FRX_DISTORTION_EFFECTS.set((float)(double) mc.options.screenEffectScale().get());
        FRX_HIDE_LIGHTNING_FLASHES.set(mc.options.hideLightningFlash().get() ? 1 : 0);
        FRX_DARKNESS_PULSING.set((float)(double) mc.options.screenEffectScale().get());
        FRX_HIGH_CONTRAST.set(mc.options.highContrast().get() ? 1 : 0);
        FRX_DAMAGE_TILT.set((float)(double) mc.options.damageTiltStrength().get());
        FRX_GLINT_STRENGTH.set((float)(double) mc.options.glintStrength().get());
        FRX_GLINT_SPEED.set((float)(double) mc.options.glintSpeed().get());

        // view.glsl
        FRX_MODEL_TO_WORLD.set((float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z, 1.0F);
        FRX_CAMERA_VIEW.set(
            Vec3.directionFromRotation(camera.xRot(), camera.yRot()).toVector3f()
        );
        FRX_VIEW_DISTANCE.set(mc.options.renderDistance().get() * 16.0F);
        FRX_VIEW_BRIGHTNESS.set(mc.options.gamma().get().floatValue());
        {
            int result = 0;

            BlockPos cameraBlockPos = BlockPos.containing(camera.position());
            Iterable<TagKey<Fluid>> fluidTags = () -> {
                return mc.level.getFluidState(cameraBlockPos).tags().iterator();
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

            CANPIPE_VIEW_FLAGS.set(result);
        }
        CANPIPE_SCREEN_SIZE.set(
            (float) mc.getWindow().getWidth(),
            (float) mc.getWindow().getHeight()
        );

        // player.glsl
        {
            float effectModifier = 0.0F;
            if (mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
                effectModifier = GameRenderer.getNightVisionScale(mc.player, 0.0F);
            }
            else if (mc.player.hasEffect(MobEffects.CONDUIT_POWER)) {
                effectModifier = mc.player.getWaterVision();
            }
            FRX_EFFECT_MODIFIER.set(effectModifier);
        }
        {
            // TODO
            /*float darknessScale = ((LightTextureExtended) mc.gameRenderer.lightmap()).canpipe_getDarknessScale();
            CANPIPE_DARKNESS_FACTOR.set(Mth.clamp(1.0f - darknessScale / 0.45f, 0.0f, 1.0f));*/
        }
        FRX_EYE_POS.set(eyePosition.toVector3f());
        FRX_EYE_BRIGHTNESS.set(lre.canpipe_getEyeBlockLight(), lre.canpipe_getEyeSkyLight());
        FRX_SMOOTHED_EYE_BRIGHTNESS.set(lre.canpipe_getSmoothedEyeBlockLight(), lre.canpipe_getSmoothedEyeSkyLight());

        Light light = ((Supplier<Light>)() -> {
            for (Item item : new Item[]{mc.player.getMainHandItem().getItem(), mc.player.getOffhandItem().getItem()}) {
                Identifier itemLocation = BuiltInRegistries.ITEM.getKey(item);
                if (itemLocation == null) return null;
                Light result = Lights.get(itemLocation);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }).get();

        {
            if (light != null) {
                FRX_HELD_LIGHT.set(light.red, light.green, light.blue, light.intensity);
                FRX_HELD_LIGHT_INNER_RADIUS.set(light.innerConeAngle);
                FRX_HELD_LIGHT_OUTER_RADIUS.set(light.outerConeAngle);
            }
            else {
                FRX_HELD_LIGHT.set(0.0F);
            }
        }
        FRX_PLAYER_MOOD.set(mc.player.getCurrentMood());
        {
            int result = 0;
            BlockPos bp = BlockPos.containing(eyePosition);
            Iterable<TagKey<Fluid>> fluidTags = ()
                -> mc.level.getFluidState(bp).tags().iterator();
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
            result |= (mc.player.isInWaterOrRain() ? 1 : 0)                       << 13;
            result |= (mc.level.getBlockState(bp).is(Blocks.POWDER_SNOW) ? 1 : 0) << 14;
            result |= (mc.player.isFreezing() ? 1 : 0)                            << 15;
            CANPIPE_PLAYER_FLAGS.set(result);
        }
        {
            long result = 0;
            result |= (mc.player.hasEffect(MobEffects.SPEED) ? 1L : 0L)               << 0;
            result |= (mc.player.hasEffect(MobEffects.SLOWNESS) ? 1L : 0L)            << 1;
            result |= (mc.player.hasEffect(MobEffects.HASTE) ? 1L : 0L)               << 2;
            result |= (mc.player.hasEffect(MobEffects.MINING_FATIGUE) ? 1L : 0L)      << 3;
            result |= (mc.player.hasEffect(MobEffects.STRENGTH) ? 1L : 0L)            << 4;
            result |= (mc.player.hasEffect(MobEffects.INSTANT_HEALTH) ? 1L : 0L)      << 5;
            result |= (mc.player.hasEffect(MobEffects.INSTANT_DAMAGE) ? 1L : 0L)      << 6;
            result |= (mc.player.hasEffect(MobEffects.JUMP_BOOST) ? 1L : 0L)          << 7;
            result |= (mc.player.hasEffect(MobEffects.NAUSEA) ? 1L : 0L)              << 8;
            result |= (mc.player.hasEffect(MobEffects.REGENERATION) ? 1L : 0L)        << 9;
            result |= (mc.player.hasEffect(MobEffects.RESISTANCE) ? 1L : 0L)          << 10;
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
            CANPIPE_EFFECTS_FLAGS.set((int)(result & 0xFFFFFFFFL), (int)(result >>> 32));
        }

        // world
        long ticks = Pipeline.getFixedTimeOrDayTime(mc.level);
        CANPIPE_FIXED_OR_DAY_TIME.set((ticks % 24000L) / 24000.0F);
        // TODO
        /*FRX_WORLD_DAY.set(mc.level != null ? (mc.level.dayTime() / 24000L) % 2147483647L : 0.0F);
        FRX_WORLD_TIME.set(mc.level != null ? (mc.level.getDayTime() % 24000L) / 24000.0F : 0.0F);*/
        FRX_MOON_SIZE.set(DimensionType.MOON_BRIGHTNESS_PER_PHASE[mc.gameRenderer.getLevelRenderState().skyRenderState.moonPhase.index()]);
        FRX_SKY_LIGHT_VECTOR.set(p.getSunOrMoonDir(mc.level, new Vector3f()));
        FRX_SKY_ANGLE_RADIANS.set(mc.gameRenderer.getLevelRenderState().skyRenderState.sunAngle);
        {
            var result = new Vector3f(0.0F);
            int color = mc.gameRenderer.getLevelRenderState().skyRenderState.sunriseAndSunsetColor;
            if (mc.level.dimensionType().hasSkyLight()) {
                result.set((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF);
                result.div(255.0F);
            }
            CANPIPE_SUNRISE_OR_SUNSET_COLOR.set(result);
        }
        {
            float skyFlashStrength = mc.gameRenderer.getLevelRenderState().skyRenderState.endFlashIntensity;
            FRX_SKY_FLASH_STRENGTH.set(skyFlashStrength);
        }
        FRX_AMBIENT_INTENSITY.set(camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, pt));
        {
            // TODO
            /*Vector4f emissiveColor = ((LightTextureExtended) mc.gameRenderer.lightTexture()).canpipe_getEmissiveColor();
            FRX_EMISSIVE_COLOR.set(emissiveColor);*/
            FRX_EMISSIVE_COLOR.set(1.0F);
        }
        {
            int value = 0;
            value |= (mc.level.dimensionType().hasSkyLight() ? 1 : 0) << 0;
            value |= (mc.level.isRaining() ? 1 : 0)                   << 1;
            value |= (mc.level.isThundering() ? 1 : 0)                << 2;
            // value |= (mc.level.dimensionType().cardinalLightType() == CardinalLightType.NETHER ? 1 : 0)  << 3;  // TODO

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
            value |= dimension << 4;

            CANPIPE_WORLD_FLAGS.set(value);
        }
        CANPIPE_WEATHER_GRADIENTS.set(
            mc.level.getRainLevel(pt),
            mc.level.getThunderLevel(pt),
            lre.canpipe_getSmoothedRainGradient(),
            lre.canpipe_getSmoothedThunderGradient()
        );

        // fog.glsl
        if (ticks == 0 && !mc.level.dimensionType().hasSkyLight()) {
            FRX_FOG_COLOR.set(1.0F);
        }
        else {
            FRX_FOG_COLOR.set(
                gre.canpipe_getFogRenderer().setupFog(
                    mc.gameRenderer.getMainCamera(),
                    mc.options.getEffectiveRenderDistance(),
                    mc.getDeltaTracker(),
                    0.0F, // mc.gameRenderer.getDarkenWorldAmount(pt), TODO
                    mc.level
                ).color
            );
        }
        FRX_FOG_ENABLED.set(1);

        Profiler.get().popPush("upload");
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {

            var builder = Std140Builder.onStack(memoryStack, ACCESSIBILITY.size());
            ACCESSIBILITY.writeTo(builder);
            commandEncoder.writeToBuffer(ACCESSIBILITY_UBO.slice(), builder.get());

            builder = Std140Builder.onStack(memoryStack, VIEW.size());
            VIEW.writeTo(builder);
            commandEncoder.writeToBuffer(VIEW_UBO.slice(), builder.get());

            builder = Std140Builder.onStack(memoryStack, SHADOW.size());
            SHADOW.writeTo(builder);
            commandEncoder.writeToBuffer(SHADOW_UBO.slice(), builder.get());

            builder = Std140Builder.onStack(memoryStack, PLAYER.size());
            PLAYER.writeTo(builder);
            commandEncoder.writeToBuffer(PLAYER_UBO.slice(), builder.get());

            builder = Std140Builder.onStack(memoryStack, WORLD.size());
            WORLD.writeTo(builder);
            commandEncoder.writeToBuffer(WORLD_UBO.slice(), builder.get());

            builder = Std140Builder.onStack(memoryStack, FOG.size());
            FOG.writeTo(builder);
            commandEncoder.writeToBuffer(FOG_UBO.slice(), builder.get());

            var buffer = memoryStack.malloc(DynamicUniforms.TRANSFORM_UBO_SIZE);
            new DynamicUniforms.Transform(
                ((GameRendererExtended) mc.gameRenderer).canpipe_worldViewMatrix(),
                new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                new Vector3f(),
                new Matrix4f()
            ).write(buffer);
            buffer.rewind();
            commandEncoder.writeToBuffer(PASS_DYNAMIC_TRANSFORMS_UBO.slice(), buffer);
        }

        Profiler.get().pop();
        Profiler.get().pop();
    }

}
