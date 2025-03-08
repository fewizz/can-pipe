package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fewizz.canpipe.light.Lights;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;

@Mixin(ReloadableResourceManager.class)
public abstract class ReloadableResourceManagerMixin {

    @Shadow abstract public void registerReloadListener(PreparableReloadListener preparableReloadListener);

    @Inject(method = "registerReloadListener", at = @At("HEAD"))
    void onRegister(PreparableReloadListener listener, CallbackInfo ci) {
        // register before models
        if (listener instanceof ModelManager) {
            registerReloadListener(new Materials());
            registerReloadListener(new MaterialMaps());
            registerReloadListener(new Lights());
            registerReloadListener(new Pipelines());
        }
    }

}
