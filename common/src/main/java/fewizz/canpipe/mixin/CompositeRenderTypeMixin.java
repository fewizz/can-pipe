package fewizz.canpipe.mixin;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.CommandEncoderExtended;
import fewizz.canpipe.mixininterface.CompositeRenderTypeExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;

@Mixin(RenderType.CompositeRenderType.class)
public class CompositeRenderTypeMixin implements CompositeRenderTypeExtended {

    @Shadow @Final private RenderPipeline renderPipeline;

    @Unique
    private RenderPipeline getReplacedRenderPipeline(RenderPipeline renderPipeline) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            Minecraft mc = Minecraft.getInstance();
            if (!((LevelRendererExtended) mc.levelRenderer).canpipe_getIsRenderingShadows()) {
                renderPipeline = p.materialPrograms.getOrDefault(renderPipeline, renderPipeline);
            }
            else {
                renderPipeline = p.shadows.materialPrograms().getOrDefault(renderPipeline, renderPipeline);
            }
        }
        return renderPipeline;
    }

    @ModifyExpressionValue(
        method = {"format", "mode", "draw"},
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/RenderType$CompositeRenderType;"+
                "renderPipeline:"+
                "Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
        )
    )
    RenderPipeline replaceRenderPipeline(RenderPipeline original) {
        return this.getReplacedRenderPipeline(original);
    }

    @Override
    public RenderPipeline canpipe_getRenderPipeline() {
        return this.getReplacedRenderPipeline(this.renderPipeline);
    }

    @WrapOperation(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass("+
                "Ljava/util/function/Supplier;"+
                "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
                "Ljava/util/OptionalInt;"+
                "Lcom/mojang/blaze3d/textures/GpuTextureView;"+
                "Ljava/util/OptionalDouble;"+
            ")Lcom/mojang/blaze3d/systems/RenderPass;"
        )
    )
    RenderPass onCreateRenderPass(
        CommandEncoder instance, Supplier<String> nameSupplier, GpuTextureView gpuTextureView, OptionalInt optionalInt, @Nullable GpuTextureView gpuTextureView2, OptionalDouble optionalDouble,
        Operation<RenderPass> operation,
        @Local RenderTarget renderTarget
    ) {
        if (renderTarget instanceof Framebuffer framebuffer) {
            return Pipelines.getCurrent().createRenderPass((CommandEncoderExtended) instance, nameSupplier, framebuffer);
        }
        return operation.call(instance, nameSupplier, gpuTextureView, optionalInt, gpuTextureView2, optionalDouble);
    }

}
