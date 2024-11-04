package fewizz.canpipe;

import fewizz.canpipe.pipeline.Pipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;

public class RenderStateShards {

    public static final RenderStateShard.OutputStateShard SOLID_TARGET = new RenderStateShard.OutputStateShard(
        "solid_target", () -> {
            Pipeline p = Mod.getCurrentPipeline();
            if (p != null) {
                p.solidTerrainFramebuffer.bindWrite(false);
            }
            else {
                Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
            }
        }, () -> {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
    );

}