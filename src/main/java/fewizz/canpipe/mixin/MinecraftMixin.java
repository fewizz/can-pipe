package fewizz.canpipe.mixin;

import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method="<init>", at=@At("TAIL"))
    void onInitEnd(CallbackInfo ci) {
        GL33C.glEnable(GL33C.GL_TEXTURE_CUBE_MAP_SEAMLESS);
    }

}
