package org.iquality.cherubin;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

public abstract class Sound {
    public final int id;
    public final String name;
    public final SysexMessage dump;
    public final SoundSet soundSet;

    public Sound(int id, String name, SysexMessage dump, SoundSet soundSet) {
        this.id = id;
        this.name = name;
        this.dump = dump;
        this.soundSet = soundSet;
        if (soundSet != null) {
            soundSet.sounds.add(this);
        }
    }

    protected abstract Sound newInstance(int id, String name, SysexMessage dump, SoundSet soundSet);

    public Sound cloneForEditBuffer() {
        return clone(id, (byte) 0x7F, (byte) 0x00);
    }

    public Sound clone(byte programBank, byte programNumber) {
        return clone(id, programBank, programNumber);
    }

    public Sound clone(int id, byte programBank, byte programNumber) {
        try {
            byte[] data = dump.getMessage(); // getMessage() returns a copy
            data[SoundCapture.PROGRAM_BANK_OFFSET] = programBank;
            data[SoundCapture.PROGRAM_NUMBER_OFFSET] = programNumber;
            return newInstance(id, name, new SysexMessage(data, data.length), null);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
