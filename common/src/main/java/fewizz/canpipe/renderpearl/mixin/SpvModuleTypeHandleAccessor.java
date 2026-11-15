package fewizz.canpipe.renderpearl.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = {"com.mojang.renderpearl.frontend.shaders.SPIRVModule$TypeHandle"})
public interface SpvModuleTypeHandleAccessor {

    @Accessor("typeHandle")
    long canpipe_getTypeHandle();

}
