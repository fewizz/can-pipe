package fewizz.canpipe.pipeline;

import com.mojang.blaze3d.systems.CommandEncoder;

public abstract class PassBase implements AutoCloseable {

    public final String name;

    PassBase(String name) {
        this.name = name;
    }


    public abstract void apply(CommandEncoder commandEncoder);

    @Override
    public abstract void close();

}
