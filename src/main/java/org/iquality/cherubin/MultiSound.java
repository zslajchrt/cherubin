package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;

public class MultiSound extends Sound {

    public MultiSound(int id, String name, SysexMessage dump, SoundSet soundSet) {
        super(id, name, dump, soundSet);
    }

    @Override
    protected MultiSound newInstance(int id, String name, SysexMessage dump, SoundSet soundSet) {
        return new MultiSound(id, name, dump, soundSet);
    }

    @Override
    public String toString() {
        return String.format("%d: %s", id, name);
    }
}
