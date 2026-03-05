package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.QuadInstance;

import fewizz.canpipe.mixininterface.QuadInstanceExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.block.ModelBlockRenderer;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {

    @Final private QuadInstance quadInstance = new QuadInstance();

    @Inject(method = "tesselateBlock", at = @At("HEAD"))
    void beforeTesselatingBlock(CallbackInfo ci) {
        ((QuadInstanceExtended) this.quadInstance).canpipe_separateScale(Pipelines.getCurrent() != null);
    }

}
