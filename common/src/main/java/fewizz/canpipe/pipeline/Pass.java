package fewizz.canpipe.pipeline;

import com.mojang.blaze3d.systems.CommandEncoder;

public abstract class Pass implements AutoCloseable {

    public final String name;

    Pass(String name) {
        this.name = name;
    }

    public abstract void apply(CommandEncoder commandEncoder);

    @Override
    public abstract void close();

}
