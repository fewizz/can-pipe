package fewizz.canpipe.pipeline;

import com.mojang.renderpearl.api.commands.CommandEncoder;

import net.minecraft.resources.Identifier;

abstract class Pass implements AutoCloseable {

    public final Identifier id;

    Pass(Identifier id) {
        this.id = id;
    }

    public abstract void apply(CommandEncoder commandEncoder);

    @Override
    public abstract void close();

}
