package fewizz.canpipe.pipeline;

public abstract class PassBase {

    public final String name;

    PassBase(String name) {
        this.name = name;
    }

    public abstract void apply();

}
