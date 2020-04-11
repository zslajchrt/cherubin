package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;

public class MultiSound extends Sound {

    public MultiSound(int id, String name, SysexMessage dump, String soundSetName) {
        super(id, name, dump, soundSetName);
    }

    @Override
    protected MultiSound newInstance(int id, String name, SysexMessage dump, String soundSetName) {
        return new MultiSound(id, name, dump, soundSetName);
    }

    @Override
    public String toString() {
        return String.format("%d: %s", id, getName());
    }
}
