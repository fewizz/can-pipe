package fewizz.canpipe.mixin.m02_texture_targets;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.blaze3d.opengl.GlStateManager;

@Mixin(value = GlStateManager.class, remap = false, priority = 1001)
public interface GlStateManagerAccessor {

    @Invoker("canpipe_setTextureTarget")
    public static void canpipe_setTextureTarget(int id, int target) {}

}
