package fewizz.canpipe.mixininterface;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public interface CompositeStateExtended {

    RenderType.CompositeState.CompositeStateBuilder canpipe_builderFromCurrentState();
    RenderStateShard.OutputStateShard canpipe_getOutputState();
    RenderType.OutlineProperty canpipe_getOutlineProperty();

}
