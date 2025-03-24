package fewizz.canpipe;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import fewizz.canpipe.light.Lights;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.pipeline.Pipelines;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public class CanPipeClientInitializer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager, Executor backgroundExecutor, Executor gameExecutor) {
                return Materials.INSTANCE.reload(barrier, manager, backgroundExecutor, gameExecutor);
            }
            @Override public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(CanPipe.MOD_ID, "materials");
            }
        });
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager, Executor backgroundExecutor, Executor gameExecutor) {
                return MaterialMaps.INSTANCE.reload(barrier, manager, backgroundExecutor, gameExecutor);
            }
            @Override public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(CanPipe.MOD_ID, "material-maps");
            }
        });
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager, Executor backgroundExecutor, Executor gameExecutor) {
                return Lights.INSTANCE.reload(barrier, manager, backgroundExecutor, gameExecutor);
            }
            @Override public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(CanPipe.MOD_ID, "lights");
            }
        });
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager, Executor backgroundExecutor, Executor gameExecutor) {
                return Pipelines.INSTANCE.reload(barrier, manager, backgroundExecutor, gameExecutor);
            }
            @Override public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(CanPipe.MOD_ID, "pipelines");
            }
        });
    }

}
