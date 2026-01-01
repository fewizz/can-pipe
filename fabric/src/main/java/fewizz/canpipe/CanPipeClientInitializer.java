package fewizz.canpipe;

import fewizz.canpipe.light.Lights;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.pipeline.Pipelines;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public class CanPipeClientInitializer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        var clientResourcesLoader = ResourceLoader.get(PackType.CLIENT_RESOURCES);

        var materials = Identifier.fromNamespaceAndPath(CanPipe.MOD_ID, "materials");
        clientResourcesLoader.registerReloader(materials,Materials.INSTANCE);

        var materialMaps = Identifier.fromNamespaceAndPath(CanPipe.MOD_ID, "material-maps");
        clientResourcesLoader.registerReloader(materialMaps, MaterialMaps.INSTANCE);

        var lights = Identifier.fromNamespaceAndPath(CanPipe.MOD_ID, "lights");
        clientResourcesLoader.registerReloader(lights, Lights.INSTANCE);

        var pipelines = Identifier.fromNamespaceAndPath(CanPipe.MOD_ID, "pipelines");
        clientResourcesLoader.registerReloader(pipelines, Pipelines.INSTANCE);

        clientResourcesLoader.addReloaderOrdering(materials, materialMaps);
        clientResourcesLoader.addReloaderOrdering(lights, pipelines);
        clientResourcesLoader.addReloaderOrdering(materialMaps, pipelines);

        KeyBindingHelper.registerKeyBinding(CanPipe.PIPELINES_RELOAD_KEY);
    }

}
