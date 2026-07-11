package fewizz.canpipe.mixin;

import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.mixininterface.QuadParticleRenderStateExtended;
import fewizz.canpipe.mixininterface.QuadParticleRenderStateStorageExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState.Storage;
import net.minecraft.client.renderer.texture.AbstractTexture;

@Mixin(QuadParticleRenderState.class)
public class QuadParticleRenderStateMixin implements QuadParticleRenderStateExtended {

    @Unique private Material canpipe_pendingMaterial;
    @Unique private short[] canpipe_materialsIndices;
    @Unique private int canpipe_forEachParticleIndex;

    @Override
    public void canpipe_setPendingMaterial(Material material) {
        this.canpipe_pendingMaterial = material;
    }

    @Shadow @Final private Map<SingleQuadParticle.Layer, Storage> particles;

    @Inject(
        method = "add",
        at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/state/level/QuadParticleRenderState$Storage.add(FFFFFFFFFFFFII)V", shift = Shift.AFTER)
    )
    void afterAdd(CallbackInfo ci, @Local SingleQuadParticle.Layer layer) {
        ((QuadParticleRenderStateStorageExtended) this.particles.get(layer)).canpipe_alsoAddMaterial(this.canpipe_pendingMaterial);
    }
/* // TODO
    @ModifyExpressionValue(
        method = "prepare",
        at = @At(
            value = "FIELD",
            target = "Lcom/mojang/blaze3d/vertex/DefaultVertexFormat;PARTICLE:Lcom/mojang/blaze3d/vertex/VertexFormat;",
            opcode = Opcodes.GETSTATIC
        )
    )
    VertexFormat replaceVertexFormat(VertexFormat vertexFormat) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            vertexFormat = ((LevelRendererExtended) Minecraft.getInstance().levelRenderer).canpipe_getCurrentShadowCascadeIdx() >= 0
                ? CanPipe.VertexFormats.PARTICLE_SHADOW
                : CanPipe.VertexFormats.PARTICLE;
        }
        return vertexFormat;
    }

    @ModifyExpressionValue(
        method = "prepare",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map$Entry;getValue()Ljava/lang/Object;",
            ordinal = 0
        )
    )
    Object onGetStorage(Object storage) {
        this.canpipe_materialsIndices = ((QuadParticleRenderStateStorageExtended) storage).canpipe_materialsValues();
        this.canpipe_forEachParticleIndex = 0;
        return storage;
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"
        ),
        index = 0
    )
    RenderPipeline replaceRenderPipeline(RenderPipeline renderPipeline) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            renderPipeline = p.getReplacedRenderPipeline(renderPipeline);
        }
        return renderPipeline;
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPass;bindTexture("+
                "Ljava/lang/String;"+
                "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
                "Lcom/mojang/blaze3d/textures/GpuSampler;"+
            ")V"
        )
    )
    void bindSpritesExtentsBeforeRender(
        CallbackInfo ci,
        @Local AbstractTexture texture,
        @Local RenderPass renderPass
    ) {
        Pipeline.bindSpritesExtentsSampler(renderPass, texture.getTextureView());
    }

    @Inject(
        method = "lambda$prepare$0",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;renderRotatedQuad(Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFFFFFII)V"
        )
    )
    void beforeRenderRotatedQuad(CallbackInfo ci, @Local(argsOnly = true) BufferBuilder bb) {
        if (bb instanceof VertexConsumerExtended vce) {
            Material material = Materials.get(this.canpipe_materialsIndices[this.canpipe_forEachParticleIndex]);
            if (material != null) {
                vce.canpipe_setPendingMaterialIndex(material.index());
            }
            ++this.canpipe_forEachParticleIndex;
        }
    }
*/
}
