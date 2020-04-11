package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;

public class SingleSound extends Sound {
    private SoundCategory category;

    public SingleSound(int id, String name, SoundCategory category, SysexMessage dump, String soundSetName) {
        super(id, name, dump, soundSetName);
        this.category = category;
    }

    synchronized public void update(String name, SysexMessage sysEx, String soundSetName, SoundCategory category) {
        update(name, sysEx, soundSetName);
        this.category = category;
    }

        @Override
    protected SingleSound newInstance(int id, String name, SysexMessage dump, String soundSetName) {
        return new SingleSound(id, name, category, dump, soundSetName);
    }

    public SoundCategory getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return String.format("%d: %s (%s)", id, getName(), category);
    }
}
