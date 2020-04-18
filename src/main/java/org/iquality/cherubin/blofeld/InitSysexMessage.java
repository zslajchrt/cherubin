package org.iquality.cherubin.blofeld;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

public class InitSysexMessage extends SysexMessage {
    public InitSysexMessage(byte[] data, int length) throws InvalidMidiDataException {
        super(data, length);
    }
}
