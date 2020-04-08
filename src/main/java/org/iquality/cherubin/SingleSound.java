package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;

public class SingleSound extends Sound {
    public final SoundCategory category;

    public SingleSound(int id, String name, SoundCategory category, SysexMessage dump, String soundSetName) {
        super(id, name, dump, soundSetName);
        this.category = category;
    }

    @Override
    protected SingleSound newInstance(int id, String name, SysexMessage dump, String soundSetName) {
        return new SingleSound(id, name, category, dump, soundSetName);
    }

    @Override
    public String toString() {
        return String.format("%d: %s (%s)", id, name, category);
    }
}
