package fewizz.canpipe.fabric;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.light.Lights;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.pipeline.Pipelines;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public class CanPipeClientInitializer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        var clientResourcesLoader = ResourceLoader.get(PackType.CLIENT_RESOURCES);

        var materials = Identifier.fromNamespaceAndPath(CanPipe.MOD_ID, "materials");
        clientResourcesLoader.registerReloadListener(materials,Materials.INSTANCE);

        var materialMaps = Identifier.fromNamespaceAndPath(CanPipe.MOD_ID, "material-maps");
        clientResourcesLoader.registerReloadListener(materialMaps, MaterialMaps.INSTANCE);

        var lights = Identifier.fromNamespaceAndPath(CanPipe.MOD_ID, "lights");
        clientResourcesLoader.registerReloadListener(lights, Lights.INSTANCE);

        var pipelines = Identifier.fromNamespaceAndPath(CanPipe.MOD_ID, "pipelines");
        clientResourcesLoader.registerReloadListener(pipelines, Pipelines.INSTANCE);

        clientResourcesLoader.addListenerOrdering(materials, materialMaps);
        clientResourcesLoader.addListenerOrdering(lights, pipelines);
        clientResourcesLoader.addListenerOrdering(materialMaps, pipelines);

        KeyMappingHelper.registerKeyMapping(CanPipe.PIPELINES_RELOAD_KEY);
    }

}
