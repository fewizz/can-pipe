package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.renderer.CompiledShaderProgram;

@Mixin(RenderSystem.class)
public interface RenderSystemAccessor {

    @Accessor("shader")
    public static void canpipe_setShader(CompiledShaderProgram program){}

    @Invoker("assertOnRenderThread")
    public static void canpipe_invokeAssertOnRenderThread() {}

}
