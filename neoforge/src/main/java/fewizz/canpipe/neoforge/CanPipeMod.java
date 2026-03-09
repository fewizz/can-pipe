package fewizz.canpipe.neoforge;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.light.Lights;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(value = "can-pipe", dist = Dist.CLIENT)
public class CanPipeMod {

    public CanPipeMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(CanPipeMod::registerReloadListeners);
        modEventBus.addListener(CanPipeMod::registerBindings);
    }

    public static void registerReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(CanPipe.MOD_ID, "materials"), Materials.INSTANCE);
        event.addListener(Identifier.fromNamespaceAndPath(CanPipe.MOD_ID, "material-maps"), MaterialMaps.INSTANCE);
        event.addListener(Identifier.fromNamespaceAndPath(CanPipe.MOD_ID, "lights"), Lights.INSTANCE);
        event.addListener(Identifier.fromNamespaceAndPath(CanPipe.MOD_ID, "pipelines"), Pipelines.INSTANCE);
    }

    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(CanPipe.PIPELINES_RELOAD_KEY);
        // event.register(CanPipe.PIPELINE_IO_DEBUG);
    }

}
