package fewizz.canpipe.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Accessor("sectionRenderDispatcher")
    void canpipe_set_sectionRenderDispatcher(SectionRenderDispatcher sectionRenderDispatcher);

}
