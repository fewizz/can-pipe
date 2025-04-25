package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.google.common.collect.ImmutableList;

import fewizz.canpipe.mixininterface.CompositeStateExtended;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.LayeringStateShard;
import net.minecraft.client.renderer.RenderStateShard.LightmapStateShard;
import net.minecraft.client.renderer.RenderStateShard.LineStateShard;
import net.minecraft.client.renderer.RenderStateShard.OverlayStateShard;
import net.minecraft.client.renderer.RenderStateShard.TexturingStateShard;

@Mixin(RenderType.CompositeState.class)
public class CompositeStateMixin implements CompositeStateExtended {

    @Shadow @Final RenderStateShard.EmptyTextureStateShard textureState;
    @Shadow @Final RenderStateShard.OutputStateShard outputState;
    @Shadow @Final RenderType.OutlineProperty outlineProperty;
    @Shadow @Final ImmutableList<RenderStateShard> states;

    @Override
    public RenderType.CompositeState.CompositeStateBuilder canpipe_builderFromCurrentState() {
        return RenderType.CompositeState.builder()
            .setTextureState(this.textureState)
            .setLightmapState((LightmapStateShard) this.states.get(1))
            .setOverlayState((OverlayStateShard) this.states.get(2))
            .setLayeringState((LayeringStateShard) this.states.get(3))
            .setOutputState(this.outputState)
            .setTexturingState((TexturingStateShard) this.states.get(5))
            .setLineState((LineStateShard) this.states.get(6));
    }

    @Override 
    public RenderStateShard.OutputStateShard canpipe_getOutputState() {
        return this.outputState;
    }

    @Override 
    public RenderType.OutlineProperty canpipe_getOutlineProperty() {
        return this.outlineProperty;
    }

}
