package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import fewizz.canpipe.mixininterface.TextureAtlasSpriteExtended;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(TextureAtlasSprite.class)
public class TextureAtlasSpriteMixin implements TextureAtlasSpriteExtended {

    @Unique int canpipe_spriteIndex = -1;

    @Override
    public int canpipe_getIndex() {
        return this.canpipe_spriteIndex;
    }

    @Override
    public void canpipe_setIndex(int index) {
        this.canpipe_spriteIndex = index;
    }

}
