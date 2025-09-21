package fewizz.canpipe;

import fewizz.canpipe.light.Lights;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.pipeline.Pipelines;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public class CanPipeClientInitializer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(
            ResourceLocation.fromNamespaceAndPath(CanPipe.MOD_ID, "materials"),
            Materials.INSTANCE
        );
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(
            ResourceLocation.fromNamespaceAndPath(CanPipe.MOD_ID, "material-maps"),
            MaterialMaps.INSTANCE
        );
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(
            ResourceLocation.fromNamespaceAndPath(CanPipe.MOD_ID, "lights"),
            Lights.INSTANCE
        );
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(
            ResourceLocation.fromNamespaceAndPath(CanPipe.MOD_ID, "pipelines"),
            Pipelines.INSTANCE
        );

        KeyBindingHelper.registerKeyBinding(CanPipe.PIPELINES_RELOAD_KEY);
        // KeyBindingHelper.registerKeyBinding(CanPipe.PIPELINE_IO_DEBUG);
    }

}
