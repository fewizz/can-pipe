package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import fewizz.canpipe.mixininterface.TextureAtlasSpriteExtended;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(TextureAtlasSprite.class)
public class TextureAtlasSpriteMixin implements TextureAtlasSpriteExtended {

    @Unique
    int spriteIndex = -1;

    @Override
    public int getIndex() {
        return this.spriteIndex;
    }

    @Override
    public void setIndex(int index) {
        this.spriteIndex = index;
    }

}
