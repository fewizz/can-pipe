package fewizz.canpipe.pipeline;

public abstract class PassBase implements AutoCloseable {

    public final String name;

    PassBase(String name) {
        this.name = name;
    }


    public abstract void apply();

    @Override
    public abstract void close();

}
