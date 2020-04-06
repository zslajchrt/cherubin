package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;

public class SingleSound extends Sound {
    public final SoundCategory category;

    public SingleSound(int id, String name, SoundCategory category, SysexMessage dump, SoundSet soundSet) {
        super(id, name, dump, soundSet);
        this.category = category;
    }

    @Override
    protected SingleSound newInstance(int id, String name, SysexMessage dump, SoundSet soundSet) {
        return new SingleSound(id, name, category, dump, soundSet);
    }

    @Override
    public String toString() {
        return String.format("%d: %s (%s)", id, name, category);
    }
}
