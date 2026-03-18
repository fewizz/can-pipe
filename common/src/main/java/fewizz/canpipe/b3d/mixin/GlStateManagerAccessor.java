package fewizz.canpipe.b3d.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.blaze3d.opengl.GlStateManager;

@Mixin(value = GlStateManager.class, remap = false, priority = 1001)
public interface GlStateManagerAccessor {

    @Invoker(value = "canpipe_setTextureTarget", remap = false)
    static void canpipe_setTextureTarget(int id, int target) {}

    @Invoker(value = "canpipe_getTextureTarget", remap = false)
    static int canpipe_getTextureTarget(int id) { return -1; }

}
