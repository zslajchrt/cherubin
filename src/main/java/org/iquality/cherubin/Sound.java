package org.iquality.cherubin;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

public abstract class Sound {
    public final int id;
    public final String name;
    public final SysexMessage sysEx;
    public final String soundSetName;

    public Sound(int id, String name, SysexMessage sysEx, String soundSetName) {
        this.id = id;
        this.name = name;
        this.sysEx = sysEx;
        this.soundSetName = soundSetName;
    }

    public boolean isEmpty() {
        return sysEx == null;
    }

    public boolean nonEmpty() {
        return !isEmpty();
    }

    protected abstract Sound newInstance(int id, String name, SysexMessage dump, String soundSetName);

    public Sound cloneForEditBuffer() {
        return clone(id, (byte) 0x7F, (byte) 0x00);
    }

    public Sound clone(byte programBank, byte programNumber) {
        return clone(id, programBank, programNumber);
    }

    public byte getBank() {
        byte[] data = sysEx.getMessage();
        return data[SoundCapture.PROGRAM_BANK_OFFSET];
    }

    public byte getSlot() {
        byte[] data = sysEx.getMessage();
        return data[SoundCapture.PROGRAM_NUMBER_OFFSET];
    }

    public Sound clone(int id, byte programBank, byte programNumber) {
        try {
            byte[] data = sysEx.getMessage(); // getMessage() returns a copy
            data[SoundCapture.PROGRAM_BANK_OFFSET] = programBank;
            data[SoundCapture.PROGRAM_NUMBER_OFFSET] = programNumber;
            return newInstance(id, name, new SysexMessage(data, data.length), soundSetName);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
