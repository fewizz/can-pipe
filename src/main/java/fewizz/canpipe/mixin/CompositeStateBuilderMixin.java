package fewizz.canpipe.mixin;

import java.util.Objects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.Pipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;

@Mixin(RenderType.CompositeState.CompositeStateBuilder.class)
public class CompositeStateBuilderMixin {

    @Shadow
    private RenderStateShard.ShaderStateShard shaderState;

    @Shadow
    private RenderStateShard.OutputStateShard outputState;

    @Inject(
        method = "createCompositeState(Lnet/minecraft/client/renderer/RenderType$OutlineProperty;)Lnet/minecraft/client/renderer/RenderType$CompositeState;",
        at = @At(value = "HEAD")
    )
    void onCreate(CallbackInfoReturnable<CompositeState> ci) {
        if (this.outputState == RenderStateShard.WEATHER_TARGET) {
            this.outputState = new CanPipe.RenderStateShards.OutputStateShard(
                "weather_target",
                this.outputState,
                (Pipeline p) -> p.weatherFramebuffer
            );
        }
        if (this.outputState == RenderStateShard.CLOUDS_TARGET) {
            this.outputState = new CanPipe.RenderStateShards.OutputStateShard(
                "clouds_target",
                this.outputState,
                (Pipeline p) -> p.cloudsFramebuffer
            );
        }

        if (!(
            this.shaderState == RenderStateShard.RENDERTYPE_SOLID_SHADER ||
            this.shaderState == RenderStateShard.RENDERTYPE_CUTOUT_MIPPED_SHADER ||
            this.shaderState == RenderStateShard.RENDERTYPE_CUTOUT_SHADER ||
            this.shaderState == RenderStateShard.RENDERTYPE_TRANSLUCENT_SHADER ||
            this.shaderState == RenderStateShard.RENDERTYPE_TRANSLUCENT_MOVING_BLOCK_SHADER ||
            this.shaderState == RenderStateShard.RENDERTYPE_ENTITY_SOLID_SHADER ||
            this.shaderState == RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_SHADER ||
            this.shaderState == RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER ||
            this.shaderState == RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET_SHADER ||
            this.shaderState == RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER ||
            this.shaderState == RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER ||
            this.shaderState == RenderStateShard.RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER ||
            this.shaderState == RenderStateShard.PARTICLE_SHADER
        )) {
            return;
        }

        if (this.outputState == RenderStateShard.TRANSLUCENT_TARGET) {
            this.shaderState = new CanPipe.RenderStateShards.MaterialProgramStateShard(
                this.shaderState,
                1,    // renderTargetIndex
                0.0F  // alphaCutout
            );
            this.outputState = new CanPipe.RenderStateShards.OutputStateShard(
                "translucent_terrain_target",
                this.outputState,
                (Pipeline p) -> Objects.requireNonNullElse(
                    (Framebuffer) Minecraft.getInstance().levelRenderer.getTranslucentTarget(),
                    p.solidFramebuffer
                )
            );
        }
        else if (this.outputState == RenderStateShard.ITEM_ENTITY_TARGET) {
            this.shaderState = new CanPipe.RenderStateShards.MaterialProgramStateShard(
                this.shaderState,
                2,    // renderTargetIndex
                0.1F  // alphaCutout
            );
            this.outputState = new CanPipe.RenderStateShards.OutputStateShard(
                "translucent_item_entity_target",
                this.outputState,
                (Pipeline p) -> Objects.requireNonNullElse(
                    (Framebuffer) Minecraft.getInstance().levelRenderer.getItemEntityTarget(),
                    p.solidFramebuffer
                )
            );
        }
        // canvas names it "translucentParticles", but applies for all particles?
        else if (this.outputState == RenderStateShard.PARTICLES_TARGET) {
            this.shaderState = new CanPipe.RenderStateShards.MaterialProgramStateShard(
                this.shaderState,
                3,    // renderTargetIndex
                0.1F  // alphaCutout
            );
            this.outputState = new CanPipe.RenderStateShards.OutputStateShard(
                "particles_target",
                this.outputState,
                (Pipeline p) -> Objects.requireNonNullElse(
                    (Framebuffer) Minecraft.getInstance().levelRenderer.getParticlesTarget(),
                    p.solidFramebuffer
                )
            );
        }
        else {
            float alphaCutout = 0.0F;

            if (
                shaderState == RenderStateShard.RENDERTYPE_CUTOUT_SHADER ||
                shaderState == RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_SHADER ||
                shaderState == RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER ||
                shaderState == RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET_SHADER ||
                shaderState == RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER ||
                shaderState == RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER
            ) {
                alphaCutout = 0.1F;
            } else
            if (shaderState == RenderStateShard.RENDERTYPE_CUTOUT_MIPPED_SHADER) {
                alphaCutout = 0.5F;
            }

            this.shaderState = new CanPipe.RenderStateShards.MaterialProgramStateShard(
                this.shaderState,
                0,  // renderTargetIndex
                alphaCutout
            );
            this.outputState = new CanPipe.RenderStateShards.OutputStateShard(
                "solid_target",
                this.outputState,
                (Pipeline p) -> p.solidFramebuffer
            );
        }
    }

}
