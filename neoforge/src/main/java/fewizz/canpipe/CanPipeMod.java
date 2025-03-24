package fewizz.canpipe;

import fewizz.canpipe.light.Lights;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

@Mod(value = "canpipe", dist = Dist.CLIENT)
public class CanPipeMod {

    public CanPipeMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(CanPipeMod::registerReloadListeners);
    }

    public static void registerReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(ResourceLocation.fromNamespaceAndPath(CanPipe.MOD_ID, "materials"), Materials.INSTANCE);
        event.addListener(ResourceLocation.fromNamespaceAndPath(CanPipe.MOD_ID, "material-maps"), MaterialMaps.INSTANCE);
        event.addListener(ResourceLocation.fromNamespaceAndPath(CanPipe.MOD_ID, "lights"), Lights.INSTANCE);
        event.addListener(ResourceLocation.fromNamespaceAndPath(CanPipe.MOD_ID, "pipelines"), Pipelines.INSTANCE);
    }

}
