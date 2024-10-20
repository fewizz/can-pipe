package fewizz.canpipe.mixin;

import java.util.Map;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import fewizz.canpipe.Mod;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

@Mixin(targets = "net.minecraft.client.renderer.GameRenderer$1")
public class GameRendererReloadListenerMixin {

    @WrapOperation(
        method = "prepare",
        at=@At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/ResourceManager;listResources(Ljava/lang/String;Ljava/util/function/Predicate;)Ljava/util/Map;")
    )
    Map<ResourceLocation, Resource> onResourceListenerPrepareListShaders(
        ResourceManager instance,
        String string, Predicate<ResourceLocation> predicate,
        Operation<Map<ResourceLocation, Resource>> original
    ) {
        var map = original.call(instance, string, predicate);
        Mod.onGameRendererReourceListenerPrepare(instance, map);
        return map;
    }

    @Inject(method = "apply", at=@At("HEAD"))
    void onResourceListenerApply(
        GameRenderer.ResourceCache resourceCache,
        ResourceManager resourceManager,
        ProfilerFiller profilerFiller,
        CallbackInfo ci
    ) {
        Mod.onGameRendererResourceListenerApply(resourceManager, resourceCache.cache());
    }


}
