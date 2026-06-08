package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;

@Mixin(BlockModelRenderState.class)
public class BlockModelRenderStateMixin {

    @Unique MaterialMap canpipe_materialMap;

    @Inject(method = "clear", at = @At("RETURN"))
    void onClear(CallbackInfo ci) {
        this.canpipe_materialMap = null;
    }

    @Inject(method = "submitModel", at = @At("HEAD"))
    void onBeforeSubmitModel(CallbackInfo ci, @Local SubmitNodeCollector submitNodeCollector) {
        ((SubmitNodeCollectorExtended) submitNodeCollector).canpipe_setPendingBlockSubmitMaterialMap(canpipe_materialMap);
    }

    @Inject(method = "submitModel", at = @At("RETURN"))
    void onAfterSubmitModel(CallbackInfo ci, @Local SubmitNodeCollector submitNodeCollector) {
        ((SubmitNodeCollectorExtended) submitNodeCollector).canpipe_setPendingBlockSubmitMaterialMap(null);
    }

}
