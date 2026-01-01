package fewizz.canpipe.mixin;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Map;

import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;

import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import fewizz.canpipe.mixininterface.TextureAtlasSpriteExtended;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin implements TextureAtlasExtended {

    @Shadow @Final private Identifier location;
    @Shadow private Map<Identifier, TextureAtlasSprite> texturesByName;
    @Shadow private int width;
    @Shadow private int height;

    @Unique GpuBuffer canpipe_spritesExtentsBuffer;

    @Inject(method = "upload", at = @At("TAIL"))
    void onUploadEnd(CallbackInfo ci) {
        if (this.width > 0x10000 || this.height > 0x10000) {
            throw new RuntimeException("Texture atlas is too big");
        }
        if (!Mth.isPowerOfTwo(this.width) || !Mth.isPowerOfTwo(this.height)) {
            throw new RuntimeException("Width or height of texture atlas is not power of 2");
        }

        ByteBuffer buff = MemoryUtil.memAlloc(texturesByName.size() * Short.BYTES*4).order(ByteOrder.LITTLE_ENDIAN);
        try {
            ShortBuffer shortBuff = buff.asShortBuffer();

            {
                int index = 0;
                for (TextureAtlasSprite s : texturesByName.values()) {
                    ((TextureAtlasSpriteExtended) s).setIndex(index);
                    shortBuff.put(index*4+0, (short) Mth.floor(s.getU0() * 0xFFFF));
                    shortBuff.put(index*4+1, (short) Mth.floor(s.getV0() * 0xFFFF));
                    shortBuff.put(index*4+2, (short) Mth.floor(s.getU1() * 0xFFFF));
                    shortBuff.put(index*4+3, (short) Mth.floor(s.getV1() * 0xFFFF));
                    index += 1;
                }
            }

            this.canpipe_spritesExtentsBuffer = RenderSystem.getDevice().createBuffer(
                () -> location.toString()+"-sprites-extents",
                GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER,
                buff
            );
        } finally {
            MemoryUtil.memFree(buff);
        }
    }

    @Inject(method = "clearTextureData", at = @At("TAIL"))
    public void onClearTextureData(CallbackInfo ci) {
        if (this.canpipe_spritesExtentsBuffer != null) {
            this.canpipe_spritesExtentsBuffer.close();
        }
    }

    @Override
    public GpuBuffer canpipe_getSpritesExtentsBuffer() {
        return this.canpipe_spritesExtentsBuffer;
    }

    @Override
    public Map<Identifier, TextureAtlasSprite> canpipe_getSprites() {
        return texturesByName;
    }

}
