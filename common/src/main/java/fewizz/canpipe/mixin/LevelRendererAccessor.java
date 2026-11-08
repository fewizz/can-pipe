package fewizz.canpipe.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Accessor("sectionRenderDispatcher")
    void canpipe_setSectionRenderDispatcher(SectionRenderDispatcher sectionRenderDispatcher);

    @Accessor("skyRenderer")
    SkyRenderer canpipe_getSkyRenderer();

    @Accessor("skyRenderer")
    void canpipe_setSkyRenderer(SkyRenderer skyRenderer);

}
