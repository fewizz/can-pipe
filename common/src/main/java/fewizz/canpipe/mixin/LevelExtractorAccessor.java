package fewizz.canpipe.mixin;

import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelExtractor.class)
public interface LevelExtractorAccessor {

    @Accessor("shouldResetSkyRenderer")
    void canpipe_set_shouldResetSkyRenderer(boolean value);

}
