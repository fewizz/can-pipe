package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.Uniforms;
import fewizz.canpipe.b3d.CommandEncoderExtended;
import fewizz.canpipe.b3d.GpuDeviceExtended;
import fewizz.canpipe.b3d.GpuTextureViewExtended;
import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;


public class Pipeline implements AutoCloseable {

    public static record Shadows(
        Map<RenderPipeline, RenderPipeline> materialPrograms,
        List<Framebuffer> framebuffers,
        List<Integer> cascadeRadii,  // for cascades 1-3, cascade 0 has max radius (render distance)
        float offsetSlopeFactor,
        float offsetBiasUnits,
        boolean supportForwardRender,  // isn't used in canvas
        boolean allowEntities,
        boolean allowParticles  // isn't used in canvas
    ) {}

    public final ResourceLocation location;
    public final Map<Option.Element<?>, Object> appliedOptions;

    public final float defaultZenithAngle;
    public final boolean smoothBrightnessBidirectionaly;
    public final int brightnessSmoothingFrames;  // I wonder why smoothing is frame dependent, not time?
    public final int rainSmoothingFrames;
    public final int thunderSmoothingFrames;

    public final Framebuffer defaultFramebuffer;
    public final Framebuffer solidFramebuffer;
    public final Framebuffer translucentTerrainFramebuffer;
    public final Framebuffer translucentItemEntityFramebuffer;
    public final Framebuffer particlesFramebuffer;
    public final Framebuffer weatherFramebuffer;
    public final Framebuffer cloudsFramebuffer;

    public final @Nullable Shadows shadows;

    public final Map<RenderPipeline, RenderPipeline> materialPrograms;
    public final Map<String, ? extends AbstractTexture> materialProgramSamplerTextures;

    private final Map<String, RenderPipeline> programs = new HashMap<>();
    private final Map<String, Texture> textures = new HashMap<>();
    private final Map<String, Framebuffer> framebuffers = new HashMap<>();

    public final List<PassBase>
        onInitPasses,
        beforeWorldRenderPasses,
        fabulousPasses,
        afterRenderHandPasses,
        onResizePasses;
    private boolean runInitPasses = true;
    private boolean runResizePasses = true;

    public Pipeline(PipelineRaw rawPipeline, Map<Option.Element<?>, Object> appliedOptions) { try {
        this.location = rawPipeline.location;
        this.appliedOptions = Collections.unmodifiableMap(appliedOptions);

        JsonObject pipelineJson = rawPipeline.getPipelineJson(appliedOptions);
        var options = rawPipeline.options;

        Function<String, Object> optionValueByName = (String name) -> {
            var element = rawPipeline.optionElementByName(name);
            if (element == null) {
                return null;
            }
            return appliedOptions.getOrDefault(element, element.defaultValue);
        };

        this.defaultZenithAngle = (float) Math.toRadians(JanksonUtils.objectOrEmpty(pipelineJson, "sky").getFloat("defaultZenithAngle", 0.0F));
        this.smoothBrightnessBidirectionaly = pipelineJson.getBoolean("smoothBrightnessBidirectionaly", false);
        this.brightnessSmoothingFrames = pipelineJson.getInt("brightnessSmoothingFrames", 20);
        this.rainSmoothingFrames = pipelineJson.getInt("rainSmoothingFrames", 500);
        this.thunderSmoothingFrames = pipelineJson.getInt("thunderSmoothingFrames", 500);

        // "images"
        Function<String, Optional<Texture>> getOrLoadOptionalTexture = (String name) -> {
            return Optional.ofNullable(this.textures.computeIfAbsent(name, _name -> {
                List<JsonObject> textures = JanksonUtils.listOfObjects(pipelineJson, "images");
                Optional<JsonObject> possibleJson = textures.stream().filter(t -> t.get(String.class, "name").equals(name)).findFirst();
                if (possibleJson.isEmpty()) {
                    return null;
                }
                return Texture.load(possibleJson.get(), location);
            }));
        };

        Function<String, Texture> getOrLoadTexture = (String name) -> {
            var result = getOrLoadOptionalTexture.apply(name);
            if (result.isEmpty()) {
                throw new RuntimeException("Couldn't find texture \""+name+"\"");
            }
            return result.get();
        };

        Function<String, Optional<? extends AbstractTexture>> getOrLoadPipelineOrResourcepackTexture = (String name) -> {
            AbstractTexture texture = null;
            if (name.contains(":")) {
                var mc = Minecraft.getInstance();
                var rl = ResourceLocation.parse(name);
                // compat, was changed in resource pack format v13
                if (rl.equals(ResourceLocation.withDefaultNamespace("textures/misc/enchanted_item_glint.png"))) {
                    rl = ItemRenderer.ENCHANTED_GLINT_ITEM;
                }
                texture = mc.getTextureManager().getTexture(rl);
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
                    Optional<JsonObject> possibleJson = framebuffers.stream().filter(t -> t.get(String.class, "name").equals(name)).findFirst();
                    if (possibleJson.isEmpty()) {
                        return null;
                    }
                    return Framebuffer.load(possibleJson.get(), location, getOrLoadTexture);
                }
                catch (Exception e) {
                    throw new RuntimeException("Error occured when tried to load framebuffer \""+name+"\"", e);
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

        JsonObject targetsJson = pipelineJson.getObject("drawTargets");

        this.defaultFramebuffer = getOrLoadFramebuffer.apply(pipelineJson.get(String.class, "defaultFramebuffer"));
        if (this.defaultFramebuffer.colorAttachmentTextures.size() != 1) {
            throw new RuntimeException("Default framebuffer \""+this.defaultFramebuffer.name+"\" has "+this.defaultFramebuffer.colorAttachmentTextures.size()+" color attachments, should have only one");
        }
        if (this.defaultFramebuffer.getDepthTexture() == null) {
            throw new RuntimeException("Default framebuffer \""+this.defaultFramebuffer.name+"\" doesn't have depth attachment");
        }

        this.solidFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "solidTerrain"));
        this.translucentTerrainFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "translucentTerrain"));
        this.translucentItemEntityFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "translucentEntity"));
        this.particlesFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "translucentParticles"));
        this.weatherFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "weather"));
        this.cloudsFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "clouds"));

        Map<ResourceLocation, String> shaderSourceCache = new HashMap<>();

        Function<ResourceLocation, Optional<String>> getShaderSource = (ResourceLocation location) -> {
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
            RenderPipelines.SOLID,
            RenderPipelines.CUTOUT_MIPPED,
            RenderPipelines.CUTOUT,
            RenderPipelines.TRANSLUCENT,
            RenderPipelines.TRIPWIRE,
            RenderPipelines.TRANSLUCENT_MOVING_BLOCK,

            RenderPipelines.ARMOR_CUTOUT_NO_CULL,
            RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL,
            RenderPipelines.ARMOR_TRANSLUCENT,
            RenderPipelines.ENTITY_SOLID,
            RenderPipelines.ENTITY_SOLID_Z_OFFSET_FORWARD,
            RenderPipelines.ENTITY_CUTOUT,
            RenderPipelines.ENTITY_CUTOUT_NO_CULL,
            RenderPipelines.ENTITY_CUTOUT_NO_CULL_Z_OFFSET,
            RenderPipelines.ENTITY_TRANSLUCENT,
            RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE,
            RenderPipelines.ENTITY_SMOOTH_CUTOUT,
            RenderPipelines.ENTITY_NO_OUTLINE,
            // RenderPipelines.BREEZE_WIND,
            // RenderPipelines.ENERGY_SWIRL,
            RenderPipelines.EYES,
            RenderPipelines.ENTITY_DECAL,
            RenderPipelines.ITEM_ENTITY_TRANSLUCENT_CULL,

            RenderPipelines.LEASH,

            RenderPipelines.OPAQUE_PARTICLE,
            RenderPipelines.TRANSLUCENT_PARTICLE
        };

        Framebuffer shadowFramebuffer = null;
        JsonObject shadowsJson = pipelineJson.getObject("skyShadows");

        if (shadowsJson != null) {
            shadowFramebuffer = getOrLoadFramebuffer.apply(shadowsJson.get(String.class, "framebuffer"));
        }

        Optional<Integer> shadowMapSize = (
            shadowFramebuffer != null ?
            Optional.of(shadowFramebuffer.getDepthTexture().getWidth(0)) :
            Optional.empty()
        );

        JsonObject materailProgram = pipelineJson.getObject("materialProgram");

        var materialVertexShaderLocation = ResourceLocation.parse(materailProgram.get(String.class, "vertexSource"));
        var materialFragmentShaderLocation = ResourceLocation.parse(materailProgram.get(String.class, "fragmentSource"));

        List<String> samplers = new ArrayList<>(JanksonUtils.listOfStrings(materailProgram, "samplers"));
        if (shadowFramebuffer != null) {
            samplers.add("frxs_shadowMap");
            samplers.add("frxs_shadowMapTexture");
        }
        this.materialPrograms = Stream.of(renderPipelines).collect(Collectors.toUnmodifiableMap(
            renderPipeline -> renderPipeline,
            renderPipeline -> MaterialPrograms.load(
                renderPipeline, glslVersion, enablePBR, false, shadowMapSize,
                materialVertexShaderLocation, materialFragmentShaderLocation,
                options, appliedOptions, samplers, getShaderSource,
                0.0F, 0.0F
            )
        ));

        var samplerImagesNames = JanksonUtils.listOfStrings(materailProgram, "samplerImages");
        Map<String, AbstractTexture> samplerToTexture = new HashMap<>();
        for (int i = 0; i < Math.min(samplers.size(), samplerImagesNames.size()); ++i) {
            String sampler = samplers.get(i);
            var samplerImage = getOrLoadPipelineOrResourcepackTexture.apply(samplerImagesNames.get(i)).get();
            samplerToTexture.put(sampler, samplerImage);
        }
        if (shadowFramebuffer != null) {
            String shadowMapTextureName = shadowFramebuffer.getDepthTexture().getLabel();
            AbstractTexture shadowMapTexture = getOrLoadPipelineOrResourcepackTexture.apply(shadowMapTextureName).get();
            samplerToTexture.put("frxs_shadowMap", shadowMapTexture);
            samplerToTexture.put("frxs_shadowMapTexture", shadowMapTexture);
        }
        this.materialProgramSamplerTextures = samplerToTexture;

        if (shadowsJson != null) {
            // Instead of one shadow framebuffer, we create N (= number of cascades) framebuffers for different layers
            List<Framebuffer> framebuffers = new ArrayList<>();
            int baseArrayLayer = ((GpuTextureViewExtended) shadowFramebuffer.getDepthTextureView()).canpipe_baseArrayLayer();
            int layerCount = ((GpuTextureViewExtended) shadowFramebuffer.getDepthTextureView()).canpipe_layerCount();
            var cascadeRadii = JanksonUtils.listOfIntegers(shadowsJson, "cascadeRadius");
            for (int i = 0; i < cascadeRadii.size() + 1; ++i) {
                // final Framebuffer fb = shadowFramebuffer;
                final int cascade = i;
                GpuTexture shadowMapTexture = shadowFramebuffer.getDepthTexture();
                GpuTextureView shadowMapTextureView = shadowFramebuffer.getDepthTextureView();
                // String shadowMapTextureName = shadowFramebuffer.getDepthTexture().getLabel();
                // Texture shadowMapTexture = getOrLoadTexture.apply(shadowMapTextureName);
                Framebuffer fb = new Framebuffer(
                    location,
                    shadowFramebuffer.name+"_"+(cascade+1),
                    () -> List.of(),// defaultFramebuffer.colorAttachments,
                    List.of(),// defaultFramebuffer.colorClearColors,
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
                    shadowFramebuffer.depthClearDepth
                );
                framebuffers.add(fb);
                this.framebuffers.put(fb.name, fb);
            }

            var vertexShaderLocation = ResourceLocation.parse(shadowsJson.get(String.class, "vertexSource"));
            var fragmentShaderLocation = ResourceLocation.parse(shadowsJson.get(String.class, "fragmentSource"));
            var materialPrograms = Stream.of(renderPipelines).collect(Collectors.toUnmodifiableMap(
                renderPipeline -> renderPipeline,
                renderPipeline -> MaterialPrograms.load(
                    renderPipeline, glslVersion, enablePBR, true, shadowMapSize,
                    vertexShaderLocation, fragmentShaderLocation, options, appliedOptions,
                    List.of(),
                    getShaderSource,
                    shadowsJson.getFloat("offsetSlopeFactor", 1.1F),
                    shadowsJson.getFloat("offsetBiasUnits", 4.0F)
                )
            ));
            this.shadows = new Shadows(
                materialPrograms,
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
        Function<String, RenderPipeline> getOrLoadProgram = (String name) -> {
            return this.programs.computeIfAbsent(name, _name -> {
                List<JsonObject> programs = JanksonUtils.listOfObjects(pipelineJson, "programs");
                JsonObject programJson = programs.stream().filter(program -> program.get(String.class, "name").equals(name)).findFirst().get();
                return Programs.load(
                    programJson, location, getShaderSource, glslVersion,
                    options, appliedOptions, shadowMapSize
                );
            });
        };

        // passes
        Function<String, List<PassBase>> loadPasses = (name) -> {
            JsonObject passesJson = pipelineJson.getObject(name);
            List<PassBase> result = new ArrayList<>();
            if (passesJson != null) {
                for (var passJson : JanksonUtils.listOfObjects(passesJson, "passes")) {
                    Pass.load(
                        passJson, optionValueByName,
                        getOrLoadOptionalFramebuffer,
                        getOrLoadProgram,
                        getOrLoadPipelineOrResourcepackTexture
                    ).ifPresent(pass -> result.add(pass));
                }
            }
            return Collections.unmodifiableList(result);
        };

        this.onInitPasses = loadPasses.apply("onInit");
        this.onResizePasses = loadPasses.apply("onResize");
        this.beforeWorldRenderPasses = loadPasses.apply("beforeWorldRender");
        this.fabulousPasses = loadPasses.apply("fabulous");
        this.afterRenderHandPasses = loadPasses.apply("afterRenderHand");
    } catch (Exception e) {
        this.close();
        throw e;
    }}

    public boolean isPassProgramRenderPipeline(RenderPipeline renderPipeline) {
        return this.programs.containsValue(renderPipeline);
    }

    public boolean isMaterialProgramRenderPipeline(RenderPipeline renderPipeline) {
        return this.materialPrograms.containsValue(renderPipeline) || (
            this.shadows != null && this.shadows.materialPrograms().containsValue(renderPipeline)
        );
    }

    public void onWindowSizeChanged(int w, int h) {
        this.textures.forEach((n, t) -> t.onWindowSizeChanged());
        this.framebuffers.forEach((n, f) -> f.onWindowSizeChanged());
        this.runResizePasses = true;
    }

    public void onBeforeWorldRender(Matrix4f view, Matrix4f projection) {
        Uniforms.updateFREXUniforms(view, projection);
        Uniforms.CANPIPE_ORIGIN_TYPE.set(0);  // camera

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            var builder = Std140Builder.onStack(memoryStack, Uniforms.MATERIAL_PROGRAM_UBO.size());
            Uniforms.MATERIAL_PROGRAM.writeTo(builder);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(Uniforms.MATERIAL_PROGRAM_UBO.slice(), builder.get());
        }

        if (this.runInitPasses) {
            for (PassBase pass : this.onInitPasses) {
                pass.apply();
            }
            this.runInitPasses = false;
        }

        if (this.runResizePasses) {
            for (PassBase pass : this.onResizePasses) {
                pass.apply();
            }
            this.runResizePasses = false;
        }

        for (PassBase pass : this.beforeWorldRenderPasses) {
            pass.apply();
        }

        Minecraft.getInstance().mainRenderTarget = this.solidFramebuffer;
    }

    public void onAfterWorldRender() {
        for (PassBase pass : this.fabulousPasses) {
            pass.apply();
        }

        Uniforms.CANPIPE_ORIGIN_TYPE.set(3);  // hands

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            var builder = Std140Builder.onStack(memoryStack, Uniforms.MATERIAL_PROGRAM_UBO.size());
            Uniforms.MATERIAL_PROGRAM.writeTo(builder);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(Uniforms.MATERIAL_PROGRAM_UBO.slice(), builder.get());
        }
    }

    public void onAfterRenderHand() {
        Minecraft.getInstance().mainRenderTarget = this.defaultFramebuffer;
        Uniforms.CANPIPE_ORIGIN_TYPE.set(2);  // screen

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            var builder = Std140Builder.onStack(memoryStack, Uniforms.MATERIAL_PROGRAM_UBO.size());
            Uniforms.MATERIAL_PROGRAM.writeTo(builder);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(Uniforms.MATERIAL_PROGRAM_UBO.slice(), builder.get());
        }

        for (PassBase pass : this.afterRenderHandPasses) {
            pass.apply();
        }
    }

    @SuppressWarnings("deprecation")
    public RenderPass createRenderPass(CommandEncoderExtended commandEncoder, Supplier<String> name, @Nullable Framebuffer framebuffer) {
        int newTarget = 0;
        if (framebuffer == this.translucentTerrainFramebuffer) {
            newTarget = 1;
        }
        if (framebuffer == this.translucentItemEntityFramebuffer) {
            newTarget = 2;
        }
        if (framebuffer == this.particlesFramebuffer) {
            newTarget = 3;
        }
        if (Uniforms.CANPIPE_RENDER_TARGET.get() != newTarget) {
            Uniforms.CANPIPE_RENDER_TARGET.set(newTarget);  // translucent render target
            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                var builder = Std140Builder.onStack(memoryStack, Uniforms.MATERIAL_PROGRAM_UBO.size());
                Uniforms.MATERIAL_PROGRAM.writeTo(builder);
                commandEncoder.writeToBuffer(Uniforms.MATERIAL_PROGRAM_UBO.slice(), builder.get());
            }
        }

        RenderPass renderPass;
        // For example, when rendering gui items
        if (RenderSystem.outputColorTextureOverride != null && RenderSystem.outputDepthTextureOverride != null) {
            renderPass = commandEncoder.createRenderPass(name, RenderSystem.outputColorTextureOverride, OptionalInt.empty(), RenderSystem.outputDepthTextureOverride, OptionalDouble.empty());
        }
        else {
            renderPass = commandEncoder.canpipe_createRenderPass(name, framebuffer.colorAttachments, framebuffer.getDepthTextureView());
        }

        renderPass.setUniform("frx_ub_accessibility", Uniforms.ACCESSIBILITY_UBO);
        renderPass.setUniform("frx_ub_view", Uniforms.VIEW_UBO);
        renderPass.setUniform("frx_ub_player", Uniforms.PLAYER_UBO);
        renderPass.setUniform("frx_ub_world", Uniforms.WORLD_UBO);
        renderPass.setUniform("frx_ub_fog", Uniforms.FOG_UBO);
        renderPass.setUniform("canpipe_ub_material_program", Uniforms.MATERIAL_PROGRAM_UBO);

        var sampler0 = RenderSystem.getShaderTexture(0);
        if (sampler0 != null) {
            var mc = Minecraft.getInstance();
            TextureAtlas atlas = null;
            for (var atlasLoc : ModelManager.VANILLA_ATLASES.keySet()) {
                var possibleAtlas = mc.getModelManager().getAtlas(atlasLoc);
                if (possibleAtlas.getTexture() == sampler0.texture()) {
                    atlas = possibleAtlas;
                    break;
                }
            }
            if (atlas == null) {  // we just need to bind something
                atlas = mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
            }
            renderPass.bindSampler("canpipe_spritesExtents", ((TextureAtlasExtended) atlas).canpipe_getSpriteData());
        }

        renderPass.bindSampler("Sampler2", Minecraft.getInstance().gameRenderer.lightTexture().getTextureView());

        for (var e : this.materialProgramSamplerTextures.entrySet()) {
            renderPass.bindSampler(e.getKey(), e.getValue().getTextureView());
        }

        return renderPass;
    }

    public Vector3f getSunOrMoonDir(Level level, Vector3f result, float partialTicks) {
        // 0.0 - noon, 0.5 - midnight
        float hourAngle = level.getSunAngle(partialTicks);
        long ticks = (level.dimensionType().fixedTime().orElse(level.getDayTime())) % 24000L;

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

    @Override
    public void close() {
        this.framebuffers.values().forEach(Framebuffer::destroyBuffers);
        this.textures.values().forEach(Texture::close);
    }

}
