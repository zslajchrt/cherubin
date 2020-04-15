package org.iquality.cherubin;

public class SynthHeader {

    private final int id;
    private final String name;
    private final SynthFactory synthFactory;

    public SynthHeader(int id, String name, SynthFactory synthFactory) {
        this.id = id;
        this.name = name;
        this.synthFactory = synthFactory;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SynthFactory getSynthFactory() {
        return synthFactory;
    }

    @Override
    public String toString() {
        return name + " (" + synthFactory.getSynthId() + ")";
    }
}
