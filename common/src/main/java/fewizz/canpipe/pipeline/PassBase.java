package fewizz.canpipe.pipeline;

import fewizz.canpipe.b3d.CommandEncoderExtended;

public abstract class PassBase implements AutoCloseable {

    public final String name;

    PassBase(String name) {
        this.name = name;
    }


    public abstract void apply(CommandEncoderExtended commandEncoder);

    @Override
    public abstract void close();

}
