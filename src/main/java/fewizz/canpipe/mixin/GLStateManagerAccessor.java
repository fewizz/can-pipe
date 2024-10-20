package fewizz.canpipe.mixin;

import org.apache.commons.lang3.NotImplementedException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.platform.GlStateManager;

@Mixin(value=GlStateManager.class, remap=false)
public interface GLStateManagerAccessor {

    @Accessor("TEXTURES")
    public static GlStateManager.TextureState[] canpipe_getTEXTURES() {
        throw new NotImplementedException();
    }

    @Accessor("activeTexture")
    public static int canpipe_getActiveTexture() {
        throw new NotImplementedException();
    }

}
