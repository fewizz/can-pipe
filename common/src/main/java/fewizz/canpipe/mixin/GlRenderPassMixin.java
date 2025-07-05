package fewizz.canpipe.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlRenderPass;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.Uniforms;
import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelManager;

@Mixin(GlRenderPass.class)
public abstract class GlRenderPassMixin {

    @Shadow public abstract void setUniform(String string, GpuBuffer gpuBuffer);
    @Shadow public abstract void bindSampler(String string, @Nullable GpuTextureView gpuTextureView);

    @Shadow protected GlRenderPipeline pipeline;

    @SuppressWarnings("deprecation")
    @WrapMethod(method = "bindSampler")
    void onBindSampler(String name, GpuTextureView textureView, Operation<Void> operation) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null && pipeline != null && p.isMaterialProgramRenderPipeline(pipeline.info())) {
            if (name.equals("Sampler0")) {
                name = "frxs_baseColor";

                var mc = Minecraft.getInstance();
                TextureAtlas atlas = null;
                for (var atlasLoc : ModelManager.VANILLA_ATLASES.keySet()) {
                    var possibleAtlas = mc.getModelManager().getAtlas(atlasLoc);
                    if (possibleAtlas.getTexture() == textureView.texture()) {
                        atlas = possibleAtlas;
                        break;
                    }
                }
                if (atlas == null) {
                    // we just need to bind something,
                    // nothin will be read from it
                    atlas = mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
                }
                operation.call(
                    "canpipe_spritesExtents",
                    ((TextureAtlasExtended) atlas).canpipe_getSpriteData()
                );
            }
            else if (name.equals("Sampler1")) {
                name = "canpipe_overlay";
            }
            else if (name.equals("Sampler2")) {
                name = "frxs_lightmap";
            }

            for (var e : p.materialProgramSamplerImages.entrySet()) {
                operation.call(e.getKey(), e.getValue());
            }
        }
        operation.call(name, textureView);
    }

    @WrapOperation(
        method = "setPipeline",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlDevice;getOrCompilePipeline("+
                "Lcom/mojang/blaze3d/pipeline/RenderPipeline;"+
            ")Lcom/mojang/blaze3d/opengl/GlRenderPipeline;"
        )
    )
    private GlRenderPipeline setPipeline(
        GlDevice instance, RenderPipeline renderPipeline,
        Operation<GlRenderPipeline> operation
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            boolean isProgram = p.isPassProgramRenderPipeline(renderPipeline);
            boolean isMaterialProgram = p.isMaterialProgramRenderPipeline(renderPipeline);

            if (isProgram || isMaterialProgram) {
                this.setUniform("frx_ub_accessibility", Uniforms.ACCESSIBILITY_UBO);
                this.setUniform("frx_ub_view", Uniforms.VIEW_UBO);
                this.setUniform("frx_ub_player", Uniforms.PLAYER_UBO);
                this.setUniform("frx_ub_world", Uniforms.WORLD_UBO);
                this.setUniform("frx_ub_fog", Uniforms.FOG_UBO);
            }

            if (isMaterialProgram) {
                this.setUniform("canpipe_ub_material_program", Uniforms.MATERIAL_PROGRAM_UBO);
                this.bindSampler("frxs_lightmap", Minecraft.getInstance().gameRenderer.lightTexture().getTextureView());
            }
        }

        return operation.call(instance, renderPipeline);
    }

}
