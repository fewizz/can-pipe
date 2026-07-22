package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPass.RenderArea;
import com.mojang.blaze3d.systems.RenderPassDescriptor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.Uniforms;
import fewizz.canpipe.b3d.GpuDeviceExtended;
import fewizz.canpipe.b3d.GpuTextureViewExtended;
import fewizz.canpipe.mixin.RenderSetupAccessor;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.level.Level;


public class Pipeline implements AutoCloseable {

    public static record Shadows(
        Map<RenderPipeline, MaterialProgramLoader> materialProgramsLoaders,
        List<Framebuffer> framebuffers,
        List<Integer> cascadeRadii,  // for cascades 1-3, cascade 0 has max radius (render distance)
        float offsetSlopeFactor,
        float offsetBiasUnits,
        boolean supportForwardRender,  // isn't used in canvas
        boolean allowEntities,
        boolean allowParticles  // isn't used in canvas
    ) {}

    public static record FabulousTargets(
        Framebuffer translucentTerrainFramebuffer,
		Framebuffer translucentItemEntityFramebuffer,
        Framebuffer translucentParticlesFramebuffer,
		Framebuffer weatherFramebuffer,
		Framebuffer cloudsFramebuffer
    ) {}

    public final Identifier location;
    public final Map<OptionGroup.Element<?>, Object> appliedOptions;

    public final float defaultZenithAngle;
    public final boolean smoothBrightnessBidirectionally;
    public final int brightnessSmoothingFrames;  // I wonder why smoothing is frame dependent, not time?
    public final int rainSmoothingFrames;
    public final int thunderSmoothingFrames;
    public final boolean awareOfDepthRangeChanges;

    public final Framebuffer defaultFramebuffer;
    public final Framebuffer solidFramebuffer;
    public final Framebuffer translucentTerrainFramebuffer;
    public final Framebuffer translucentItemEntityFramebuffer;
    public final Framebuffer translucentParticlesFramebuffer;
    public final Framebuffer weatherFramebuffer;
    public final Framebuffer cloudsFramebuffer;

    public final @Nullable Shadows shadows;
    public final @Nullable FabulousTargets fabulousTargets;

    private final Map<RenderPipeline, MaterialProgramLoader> materialProgramsLoaders;
    private final Map<String, ? extends AbstractTexture> materialProgramSamplerTextures;

    private final Map<Pair<String, Pair<List<GpuFormat>, GpuFormat>>, RenderPipeline> programs = new HashMap<>();
    private final Map<String, Texture> textures = new HashMap<>();
    private final Map<String, Framebuffer> framebuffers = new HashMap<>();

    public final Map<Pair<GpuFormat, RenderPipeline>, RenderPipeline> replacedRenderPipelines = new HashMap<>();

    private final List<Pass>
        onInitPasses = new ArrayList<>(),
        beforeWorldRenderPasses = new ArrayList<>(),
        fabulousPasses = new ArrayList<>(),
        afterRenderHandPasses = new ArrayList<>(),
        onResizePasses = new ArrayList<>();

    Pipeline(PipelineRaw rawPipeline, Map<OptionGroup.Element<?>, Object> appliedOptions) { try {
        this.location = rawPipeline.location;
        this.appliedOptions = Collections.unmodifiableMap(appliedOptions);

        JsonObject pipelineJson = rawPipeline.getPipelineJson(appliedOptions);

        PipelinesFixes.fix(this.location, pipelineJson);

        var options = rawPipeline.options;

        Function<String, Object> optionValueByName = (String name) -> {
            var element = rawPipeline.optionElementByName(name);
            if (element == null) {
                return null;
            }
            return appliedOptions.getOrDefault(element, element.defaultValue);
        };

        this.defaultZenithAngle = (float) Math.toRadians(JanksonUtils.objectOrEmpty(pipelineJson, "sky").getFloat("defaultZenithAngle", 0.0F));
        this.smoothBrightnessBidirectionally = pipelineJson.getBoolean("smoothBrightnessBidirectionaly", false);
        this.brightnessSmoothingFrames = pipelineJson.getInt("brightnessSmoothingFrames", 20);
        this.rainSmoothingFrames = pipelineJson.getInt("rainSmoothingFrames", 500);
        this.thunderSmoothingFrames = pipelineJson.getInt("thunderSmoothingFrames", 500);
        this.awareOfDepthRangeChanges = rawPipeline.awareOfDepthRangeChanges;

        // "images"
        Function<String, Optional<Texture>> getOrLoadOptionalTexture = (String name) -> {
            return Optional.ofNullable(this.textures.computeIfAbsent(name, _name -> {
                List<JsonObject> textures = JanksonUtils.listOfObjects(pipelineJson, "images");
                Optional<JsonObject> possibleJson = textures.stream().filter(t -> JanksonUtils.stringOrThrow(t, "name").equals(name)).findFirst();
                if (possibleJson.isEmpty()) {
                    return null;
                }
                try {
                    return Texture.load(possibleJson.get(), location);
                } catch(Exception e) {
                    throw new RuntimeException("Couldn't load texture \""+name+"\"", e);
                }
            }));
        };

        Function<String, Texture> getOrLoadTexture = (String name) -> {
            var result = getOrLoadOptionalTexture.apply(name);
            if (result.isEmpty()) {
                throw new RuntimeException("Couldn't find texture \""+name+"\"");
            }
            return result.get();
        };

        Function<String, Optional<AbstractTexture>> getOrLoadPipelineOrResourcepackTexture = (String name) -> {
            AbstractTexture texture = null;
            if (name.contains(":")) {
                var mc = Minecraft.getInstance();
                var id = CanPipe.upgradeIdentifier(Identifier.parse(name));

                if (id.equals(Identifier.withDefaultNamespace("textures/environment/moon_phases.png"))) {
                    texture = new MoonPhasesTexture();
                }
                if (texture == null) {
                    texture = mc.getTextureManager().getTexture(id);
                }
            }
            else {
                texture = getOrLoadOptionalTexture.apply(name).orElse(null);
            }
            return Optional.ofNullable(texture);
        };

        // "framebuffers"
        Function<String, Optional<Framebuffer>> getOrLoadOptionalFramebuffer = (String name) -> {
            return Optional.ofNullable(this.framebuffers.computeIfAbsent(name, _name -> {
                try {
                    List<JsonObject> framebuffers = JanksonUtils.listOfObjects(pipelineJson, "framebuffers");
                    Optional<JsonObject> possibleJson = framebuffers.stream().filter(t -> JanksonUtils.stringOrThrow(t, "name").equals(name)).findFirst();
                    if (possibleJson.isEmpty()) {
                        return null;
                    }
                    return Framebuffer.load(possibleJson.get(), location, getOrLoadTexture);
                }
                catch (Exception e) {
                    throw new RuntimeException("Couldn't load framebuffer \""+name+"\"", e);
                }
            }));
        };

        Function<String, Framebuffer> getOrLoadFramebuffer = (String name) -> {
            var result = getOrLoadOptionalFramebuffer.apply(name);
            if (result.isEmpty()) {
                throw new RuntimeException("Couldn't find framebuffer \""+name+"\"");
            }
            return result.get();
        };

        this.defaultFramebuffer = getOrLoadFramebuffer.apply(pipelineJson.get(String.class, "defaultFramebuffer"));
        if (this.defaultFramebuffer.colorTextures.length != 1) {
            throw new RuntimeException("Default framebuffer \""+this.defaultFramebuffer.name+"\" has "+this.defaultFramebuffer.colorTextures.length+" color attachments, should have only one");
        }
        if (this.defaultFramebuffer.getDepthTexture() == null) {
            throw new RuntimeException("Default framebuffer \""+this.defaultFramebuffer.name+"\" doesn't have depth attachment");
        }

        JsonObject targetsJson = JanksonUtils.objectOrThrow(pipelineJson, "drawTargets");
        this.solidFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "solidTerrain"));
        this.translucentTerrainFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "translucentTerrain"));
        this.translucentItemEntityFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "translucentEntity"));
        this.translucentParticlesFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "translucentParticles"));
        this.weatherFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "weather"));
        this.cloudsFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "clouds"));

        JsonObject fabulousTargetsJson = pipelineJson.getObject("fabulousTargets");
        this.fabulousTargets = fabulousTargetsJson == null ? null : new FabulousTargets(
            getOrLoadFramebuffer.apply(fabulousTargetsJson.get(String.class, "translucent")),
            getOrLoadFramebuffer.apply(fabulousTargetsJson.get(String.class, "entity")),
            getOrLoadFramebuffer.apply(fabulousTargetsJson.get(String.class, "particles")),
            getOrLoadFramebuffer.apply(fabulousTargetsJson.get(String.class, "weather")),
            getOrLoadFramebuffer.apply(fabulousTargetsJson.get(String.class, "clouds"))
        );
        if (pipelineJson.containsKey("fabulous") && this.fabulousTargets == null) {
            throw new RuntimeException("To use \"fabulous\" passes, \"fabulousTargets\" should be defined");
        }

        Map<Identifier, String> shaderSourceCache = new HashMap<>();

        Function<Identifier, Optional<String>> getShaderSource = (Identifier location) -> {
            String source = shaderSourceCache.computeIfAbsent(location, (loc) -> {
                try {
                    Minecraft mc = Minecraft.getInstance();
                    var resource = mc.getResourceManager().getResource(location);
                    if (resource.isEmpty()) {
                        return null;
                    }
                    return IOUtils.toString(resource.get().openAsReader());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return Optional.ofNullable(source);
        };

        // "materialProgram"
        boolean enablePBR = pipelineJson.getBoolean("enablePBR", false);
        int glslVersion = pipelineJson.getInt("glslVersion", 330);

        var renderPipelines = new RenderPipeline[] {
            RenderPipelines.SOLID_BLOCK,
            RenderPipelines.SOLID_TERRAIN,
            RenderPipelines.CUTOUT_BLOCK,
            RenderPipelines.CUTOUT_TERRAIN,
            RenderPipelines.TRANSLUCENT_TERRAIN,
            RenderPipelines.BEACON_BEAM_OPAQUE,
            RenderPipelines.BEACON_BEAM_TRANSLUCENT,
            RenderPipelines.TRANSLUCENT_BLOCK,

            RenderPipelines.ARMOR_CUTOUT_NO_CULL,
            RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL,
            RenderPipelines.ARMOR_TRANSLUCENT,

            RenderPipelines.ENTITY_SOLID,
            RenderPipelines.ENTITY_SOLID_Z_OFFSET_FORWARD,
            RenderPipelines.ENTITY_CUTOUT,
            RenderPipelines.ENTITY_CUTOUT_CULL,
            RenderPipelines.ENTITY_CUTOUT_Z_OFFSET,
            RenderPipelines.ENTITY_CUTOUT_DISSOLVE,
            RenderPipelines.ENTITY_TRANSLUCENT,
            RenderPipelines.ENTITY_TRANSLUCENT_CULL,
            RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE,
            RenderPipelines.BANNER_PATTERN,

            RenderPipelines.ITEM_CUTOUT,
            RenderPipelines.ITEM_TRANSLUCENT,

            RenderPipelines.EYES,

            RenderPipelines.LEASH,

            RenderPipelines.OPAQUE_PARTICLE,
            RenderPipelines.TRANSLUCENT_PARTICLE,

            RenderPipelines.TEXT,
            RenderPipelines.TEXT_BACKGROUND,
            RenderPipelines.TEXT_POLYGON_OFFSET
        };

        Framebuffer shadowFramebuffer = null;
        JsonObject shadowsJson = pipelineJson.getObject("skyShadows");

        if (shadowsJson != null) {
            shadowFramebuffer = getOrLoadFramebuffer.apply(shadowsJson.get(String.class, "framebuffer"));
            Objects.requireNonNull(shadowFramebuffer.getDepthTexture());
            Objects.requireNonNull(shadowFramebuffer.getDepthTextureView());
        }

        Optional<Integer> shadowMapSize = (
            shadowFramebuffer != null ?
            Optional.of(shadowFramebuffer.getDepthTexture().getWidth(0)) :
            Optional.empty()
        );

        JsonObject materialProgram = JanksonUtils.objectOrThrow(pipelineJson, "materialProgram");
        var materialVertexShaderLocation = Identifier.parse(JanksonUtils.stringOrThrow(materialProgram, "vertexSource"));
        var materialFragmentShaderLocation = Identifier.parse(JanksonUtils.stringOrThrow(materialProgram, "fragmentSource"));

        List<String> samplers = new ArrayList<>(JanksonUtils.listOfStrings(materialProgram, "samplers"));
        if (shadowFramebuffer != null) {
            samplers.add("frxs_shadowMap");
            samplers.add("frxs_shadowMapTexture");
        }
        this.materialProgramsLoaders = Stream.of(renderPipelines).collect(Collectors.toUnmodifiableMap(
            renderPipeline -> renderPipeline,
            renderPipeline -> MaterialPrograms.load(
                renderPipeline, glslVersion, enablePBR, false, shadowMapSize,
                materialVertexShaderLocation, materialFragmentShaderLocation,
                options, appliedOptions, samplers, getShaderSource,
                0.0F, 0.0F,
                this.awareOfDepthRangeChanges
            )
        ));

        var samplerImagesNames = JanksonUtils.listOfStrings(materialProgram, "samplerImages");
        Map<String, AbstractTexture> samplerToTexture = new HashMap<>();
        for (int i = 0; i < Math.min(samplers.size(), samplerImagesNames.size()); ++i) {
            String sampler = samplers.get(i);
            var samplerImage = getOrLoadPipelineOrResourcepackTexture.apply(samplerImagesNames.get(i));
            if (samplerImage.isEmpty()) {
                throw new RuntimeException("Couldn't find material program sampler image \""+samplerImagesNames.get(i)+"\"");
            }
            samplerToTexture.put(sampler, samplerImage.get());
        }
        if (shadowFramebuffer != null) {
            String shadowMapTextureName = shadowFramebuffer.getDepthTexture().getLabel();
            Optional<AbstractTexture> shadowMapTexture = getOrLoadPipelineOrResourcepackTexture.apply(shadowMapTextureName);
            if (shadowMapTexture.isEmpty()) {
                throw new RuntimeException("Couldn't find material program shadowmap image \""+shadowMapTextureName+"\"");
            }
            samplerToTexture.put("frxs_shadowMap", shadowMapTexture.get());
            samplerToTexture.put("frxs_shadowMapTexture", shadowMapTexture.get());
        }
        this.materialProgramSamplerTextures = samplerToTexture;

        if (shadowsJson != null) {
            // Instead of one shadow framebuffer, we create N (= number of cascades) framebuffers for different layers
            List<Framebuffer> framebuffers = new ArrayList<>();
            int baseArrayLayer = ((GpuTextureViewExtended) shadowFramebuffer.getDepthTextureView()).canpipe_baseArrayLayer();
            int layerCount = ((GpuTextureViewExtended) shadowFramebuffer.getDepthTextureView()).canpipe_layerCount();
            var cascadeRadii = JanksonUtils.listOfIntegers(shadowsJson, "cascadeRadius");
            for (int i = 0; i < cascadeRadii.size() + 1; ++i) {
                final int cascade = i;
                GpuTexture shadowMapTexture = shadowFramebuffer.getDepthTexture();
                GpuTextureView shadowMapTextureView = shadowFramebuffer.getDepthTextureView();
                Framebuffer fb = new Framebuffer(
                    location,
                    shadowFramebuffer.name+"_"+(cascade+1),
                    (idx) -> { throw new RuntimeException("Color texture getter should not be called"); },
                    new Vector4f[]{},  // clear colors
                    () -> {
                        var shadowMapCascadeTextureView = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                            shadowMapTexture,
                            shadowMapTextureView.baseMipLevel(),
                            shadowMapTextureView.mipLevels(),
                            baseArrayLayer + layerCount * cascade,
                            layerCount
                        );
                        return Pair.of(shadowMapTexture, shadowMapCascadeTextureView);
                    },
                    shadowFramebuffer.depthTextureClearDepth
                );
                framebuffers.add(fb);
                this.framebuffers.put(fb.name, fb);
            }

            var vertexShaderLocation = Identifier.parse(JanksonUtils.stringOrThrow(shadowsJson, "vertexSource"));
            var fragmentShaderLocation = Identifier.parse(JanksonUtils.stringOrThrow(shadowsJson, "fragmentSource"));
            var materialProgramsLoaders = Stream.of(renderPipelines).collect(Collectors.toUnmodifiableMap(
                renderPipeline -> renderPipeline,
                renderPipeline -> MaterialPrograms.load(
                    renderPipeline, glslVersion, enablePBR, true, shadowMapSize,
                    vertexShaderLocation, fragmentShaderLocation, options, appliedOptions,
                    List.of(),
                    getShaderSource,
                    shadowsJson.getFloat("offsetSlopeFactor", 1.1F),
                    shadowsJson.getFloat("offsetBiasUnits", 4.0F),
                    this.awareOfDepthRangeChanges
                )
            ));
            this.shadows = new Shadows(
                materialProgramsLoaders,
                framebuffers,
                cascadeRadii,
                shadowsJson.getFloat("offsetSlopeFactor", 1.1F),
                shadowsJson.getFloat("offsetBiasUnits", 4.0F),
                shadowsJson.getBoolean("supportForwardRender", true),
                shadowsJson.getBoolean("allowEntities", true),
                shadowsJson.getBoolean("allowParticles", true)
            );
        }
        else {
            this.shadows = null;
        }

        // "programs"
        BiFunction<String, Framebuffer, RenderPipeline> getOrLoadProgram = (String name, Framebuffer fb) -> {
            return this.programs.computeIfAbsent(Pair.of(name, fb.getFormats()), _name -> {
                List<JsonObject> programs = JanksonUtils.listOfObjects(pipelineJson, "programs");
                Optional<JsonObject> programJson = programs.stream().filter(program -> JanksonUtils.stringOrThrow(program, "name").equals(name)).findFirst();
                if (programJson.isEmpty()) {
                    throw new RuntimeException("Couldn't find program \""+name+"\"");
                }
                try {
                    return Programs.load(
                        programJson.get(), location, getShaderSource, glslVersion,
                        options, appliedOptions, shadowMapSize,
                        fb
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Couldn't load program \""+name+"\"", e);
                }
            });
        };

        // passes
        BiConsumer<String, List<Pass>> loadPasses = (name, passes) -> {
            JsonObject passesJson = pipelineJson.getObject(name);
            if (passesJson != null) {
                for (var passJson : JanksonUtils.listOfObjects(passesJson, "passes")) {
                    ProgramPass.load(
                        location,
                        passJson, optionValueByName,
                        getOrLoadOptionalFramebuffer,
                        getOrLoadProgram,
                        getOrLoadPipelineOrResourcepackTexture
                    ).ifPresent(passes::add);
                }
            }
        };

        loadPasses.accept("onInit", this.onInitPasses);
        loadPasses.accept("onResize", this.onResizePasses);
        loadPasses.accept("beforeWorldRender", this.beforeWorldRenderPasses);
        loadPasses.accept("fabulous", this.fabulousPasses);
        loadPasses.accept("afterRenderHand", this.afterRenderHandPasses);
    } catch (Exception e) {
        this.close();
        throw e;
    }}

    @Override
    public void close() {
        this.onInitPasses.forEach(Pass::close);
        this.onResizePasses.forEach(Pass::close);
        this.beforeWorldRenderPasses.forEach(Pass::close);
        this.fabulousPasses.forEach(Pass::close);
        this.afterRenderHandPasses.forEach(Pass::close);

        this.framebuffers.values().forEach(Framebuffer::destroyBuffers);
        this.textures.values().forEach(Texture::close);
    }

    public void onWindowSizeChanged(int w, int h) {
        this.textures.forEach((n, t) -> t.onWindowSizeChanged());
        this.framebuffers.forEach((n, f) -> f.onWindowSizeChanged());
    }

    public void onBeforeRenderingLevel(Matrix4fc view, Matrix4fc projection, boolean runResizePasses, boolean runInitPasses) {
        Profiler.get().push("can-pipe before world");

        Minecraft mc = Minecraft.getInstance();
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            var buffer = memoryStack.malloc(DynamicUniforms.TRANSFORM_UBO_SIZE);
            new DynamicUniforms.Transform(
                ((GameRendererExtended) mc.gameRenderer).canpipe_worldViewMatrix(),
                new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                new Vector3f(),
                new Matrix4f()
            ).write(buffer);
            buffer.rewind();
            commandEncoder.writeToBuffer(ProgramPass.DYNAMIC_TRANSFORMS_UBO.slice(), buffer);
        }

        if (runInitPasses) {
            for (Pass pass : this.onInitPasses) {
                pass.apply(commandEncoder);
            }
        }

        if (runResizePasses) {
            for (Pass pass : this.onResizePasses) {
                pass.apply(commandEncoder);
            }
        }

        for (Pass pass : this.beforeWorldRenderPasses) {
            pass.apply(commandEncoder);
        }

        Profiler.get().pop();
    }

    public void onAfterWorldRender() {
        Profiler.get().push("can-pipe after world");

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        for (Pass pass : this.fabulousPasses) {
            pass.apply(commandEncoder);
        }

        Profiler.get().pop();
    }

    public void onAfterRenderHand() {
        Profiler.get().push("can-pipe after hand");

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        for (Pass pass : this.afterRenderHandPasses) {
            pass.apply(commandEncoder);
        }

        Profiler.get().pop();
    }

    public MaterialProgramLoader getMaterialProgramLoader(RenderPipeline renderPipeline) {
        Minecraft mc = Minecraft.getInstance();

        if (this.shadows != null && ((LevelRendererExtended) mc.levelRenderer).canpipe_getCurrentShadowCascadeIdx() >= 0) {
            return this.shadows.materialProgramsLoaders().get(renderPipeline);
        }
        else {
            return this.materialProgramsLoaders.get(renderPipeline);
        }
    }

    public RenderPipeline getReplacedRenderPipeline(RenderPipeline renderPipeline, Pair<List<GpuFormat>, GpuFormat> formats) {
        MaterialProgramLoader loader = this.getMaterialProgramLoader(renderPipeline);

        if (loader != null) {
            renderPipeline = loader.getOrCompileRenderPipeline(formats);
        }

        return renderPipeline;
    }

    public RenderPass createRenderPass(CommandEncoder commandEncoder, Supplier<String> name, Framebuffer framebuffer) {
        GameRendererExtended gre = (GameRendererExtended) Minecraft.getInstance().gameRenderer;
        LevelRendererExtended lre = (LevelRendererExtended) Minecraft.getInstance().levelRenderer;

        var descriptor = RenderPassDescriptor.create(name);
        for (int i = 0; i < framebuffer.colorTextureViews.length; ++i) {
            var colorAttachment = framebuffer.colorTextureViews[i];
            if (RenderSystem.outputColorTextureOverride != null) {
                colorAttachment = i == 0 ? RenderSystem.outputColorTextureOverride : null;
            }

            if (colorAttachment != null) {
                descriptor.withColorAttachment(colorAttachment);
            }
            else {
                descriptor.withUnusedColorAttachment();
            }
        }

        var depthAttachment = framebuffer.getDepthTextureView();
        if (RenderSystem.outputDepthTextureOverride != null) {
            depthAttachment = RenderSystem.outputDepthTextureOverride;
        }
        descriptor.withDepthAttachment(depthAttachment);

        RenderArea renderArea;
        if (RenderSystem.outputColorTextureOverride != null) {
            renderArea = new RenderArea(0, 0, RenderSystem.outputColorTextureOverride.getWidth(0), RenderSystem.outputColorTextureOverride.getHeight(0));
        }
        else {
            renderArea = new RenderArea(0, 0, framebuffer.width, framebuffer.height);
        }

        descriptor.withRenderArea(renderArea);
        RenderPass renderPass = commandEncoder.createRenderPass(descriptor);

        Uniforms.setRenderPassFREXUniforms(renderPass);

        renderPass.setUniform("canpipe_ub_origin_type", CanPipe.get0to3UBOBuffers()[gre.canpipe_getOriginType()]);
        renderPass.setUniform("canpipe_ub_is_rendering_hand", CanPipe.get0to3UBOBuffers()[gre.canpipe_isRenderingHand() ? 1 : 0]);
        if (lre.canpipe_getCurrentShadowCascadeIdx() >= 0) {
            renderPass.setUniform("frxu_ub_cascade", CanPipe.get0to3UBOBuffers()[lre.canpipe_getCurrentShadowCascadeIdx()]);
        }

        int renderTarget = 0;
        if (framebuffer == this.translucentTerrainFramebuffer) {
            renderTarget = 1;
        }
        if (framebuffer == this.translucentItemEntityFramebuffer) {
            renderTarget = 2;
        }
        if (framebuffer == this.translucentParticlesFramebuffer) {
            renderTarget = 3;
        }

        renderPass.setUniform("canpipe_ub_render_target", CanPipe.get0to3UBOBuffers()[renderTarget]);

        // We don't know yet which render pipeline will be used
        renderPass.bindTexture("Sampler0", CanPipe.getWhiteTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        renderPass.bindTexture("Sampler1", Minecraft.getInstance().gameRenderer.overlayTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        renderPass.bindTexture("Sampler2", Minecraft.getInstance().gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));

        for (var e : this.materialProgramSamplerTextures.entrySet()) {
            AbstractTexture texture = e.getValue();
            renderPass.bindTexture(e.getKey(), texture.getTextureView(), texture.getSampler());
        }

        return renderPass;
    }

    public static void bindSpritesExtentsSampler(RenderPass renderPass, GpuTextureView sampler0) {
        var mc = Minecraft.getInstance();
        MutableObject<TextureAtlas> atlas = new MutableObject<>();

        mc.getAtlasManager().forEach((loc, possibleAtlas) -> {
            if (atlas.get() == null && possibleAtlas.getTexture() == sampler0.texture()) {
                atlas.setValue(possibleAtlas);
            }
        });
        if (atlas.get() == null) {  // we just need to bind something
            atlas.setValue(mc.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS));
        }
        renderPass.setUniform(
            "canpipe_spritesExtents",
            ((TextureAtlasExtended) atlas.get()).canpipe_getSpritesExtentsBuffer()
        );
    }

    public Vector3f getSunOrMoonDir(Level level, Vector3f result) {
        // 0.0 - noon, 0.5 - midnight
        float hourAngle = Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.skyRenderState.sunAngle;
        long ticks = level.getDefaultClockTime() % 24000L;

        result.set(
            (float) (-Math.sin(hourAngle)),
            (float) ( Math.cos(hourAngle) *  Math.cos(this.defaultZenithAngle)),
            (float) ( Math.cos(hourAngle) * -Math.sin(this.defaultZenithAngle))
        );

        if (ticks > 13000L && ticks < 23000L) {  // moon
            result.negate();
        }

        return result;
    }

    public Framebuffer shadowFramebufferOr(Framebuffer or) {
        int cascade = ((LevelRendererExtended) Minecraft.getInstance().levelRenderer).canpipe_getCurrentShadowCascadeIdx();
        return cascade >= 0 ? this.shadows.framebuffers.get(cascade) : or;
    }

    public RenderTarget replaceRenderTarget(RenderTarget renderTarget, RenderSetup renderSetup) {
        RenderPipeline originalRenderPipeline = ((RenderSetupAccessor) (Object) renderSetup).canpipe_getPipeline();

        MaterialProgramLoader loader = this.getMaterialProgramLoader(originalRenderPipeline);
        boolean renderPipelineIsReplaced = loader != null;

        if (renderPipelineIsReplaced) {
            OutputTarget outputTarget = ((RenderSetupAccessor) (Object) renderSetup).canpipe_getOutputTarget();
            if (outputTarget == OutputTarget.MAIN_TARGET) {
                renderTarget = this.shadowFramebufferOr(this.solidFramebuffer);
            }
            else if (outputTarget == OutputTarget.ITEM_ENTITY_TARGET) {
                renderTarget = this.shadowFramebufferOr(this.translucentItemEntityFramebuffer);
            }
            else if (outputTarget == OutputTarget.WEATHER_TARGET) {
                renderTarget = this.shadowFramebufferOr(this.weatherFramebuffer);
            }
            else {
                throw new RuntimeException("Unexpected output target: "+outputTarget);
            }
        }

        return renderTarget;
    }

    public RenderTarget replaceRenderTarget(RenderTarget renderTarget, RenderPipeline originalRenderPipeline, OutputTarget outputTarget) {

        MaterialProgramLoader loader = this.getMaterialProgramLoader(originalRenderPipeline);
        boolean renderPipelineIsReplaced = loader != null;

        if (renderPipelineIsReplaced) {
            if (outputTarget == OutputTarget.MAIN_TARGET) {
                renderTarget = this.shadowFramebufferOr(this.solidFramebuffer);
            }
            else if (outputTarget == OutputTarget.ITEM_ENTITY_TARGET) {
                renderTarget = this.shadowFramebufferOr(this.translucentItemEntityFramebuffer);
            }
            else if (outputTarget == OutputTarget.WEATHER_TARGET) {
                renderTarget = this.shadowFramebufferOr(this.weatherFramebuffer);
            }
            else {
                throw new RuntimeException("Unexpected output target: "+outputTarget);
            }
        }

        return renderTarget;
    }

    public RenderPipeline onRenderPassSetPipeline(RenderPipeline pipeline, List<RenderPassDescriptor.Attachment<Optional<Vector4fc>>> colorAttachments) {
        if (!pipeline.getLocation().getNamespace().equals("minecraft")) { return pipeline; }

        boolean patchReversedDepth = !this.awareOfDepthRangeChanges;
        boolean patchAttachment = !colorAttachments.isEmpty() && colorAttachments.getFirst().textureView().texture().getFormat() != GpuFormat.RGBA8_UNORM;  //this.defaultFramebuffer.getColorTexture().getFormat() != GpuFormat.RGBA8_UNORM;

        if (!patchAttachment && !patchReversedDepth) { return pipeline;}

        GpuFormat firstColorAttachmentFormat = colorAttachments.isEmpty() ? null : colorAttachments.get(0).textureView().texture().getFormat();

        return this.replacedRenderPipelines.computeIfAbsent(Pair.of(firstColorAttachmentFormat, pipeline), _key -> {
            for (var a : colorAttachments) {
                System.out.println("\t"+a.textureView().texture().getLabel() + " "+a.textureView().texture().getFormat());
            }

            ColorTargetState[] colorTargets = pipeline.getColorTargetStates();
            if (patchAttachment) {
                List<ColorTargetState> colorTargetsList = new ArrayList<>();
                for (var attachment : colorAttachments) {
                    colorTargetsList.add(new ColorTargetState(
                            pipeline.getColorTargetState().blendFunction(),
                            attachment.textureView().texture().getFormat(),
                            pipeline.getColorTargetState().writeMask()
                    ));
                }
                colorTargets = colorTargetsList.toArray(new ColorTargetState[0]);
            }

            DepthStencilState depthState = pipeline.getDepthStencilState();
            if (patchReversedDepth && depthState != null) {
                depthState = new DepthStencilState(
                    CanPipe.reverseCompareOp(depthState.depthTest()),
                    depthState.writeDepth(),
                    depthState.depthBiasScaleFactor(),
                    depthState.depthBiasConstant()
                );
            }

            return new RenderPipeline(
                pipeline.getLocation(),
                pipeline.getVertexShader(),
                pipeline.getFragmentShader(),
                pipeline.getShaderDefines(),
                pipeline.getBindGroupLayouts(),
                colorTargets,
                depthState,
                pipeline.getPolygonMode(),
                pipeline.isCull(),
                pipeline.getVertexFormatBindings(),
                pipeline.getPrimitiveTopology(),
                pipeline.getSortKey()
            ) {};
        });
    }

}