package org.iquality.cherubin;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import java.io.IOException;
import java.io.InputStream;

public interface BlofeldSoundCommon extends Sound {

    int MESSAGE_ID_OFFSET = 4;
    int BANK_OFFSET = 5;
    int PROGRAM_OFFSET = 6;
    int SDATA_OFFSET = 7;
    int SDATA_LENGTH = 380;
    int SINGLE_NAME_OFFSET = 363;
    int NAME_LENGTH = 16;
    int SINGLE_CAT_OFFSET = 379;
    int MULTI_NAME_OFFSET = SDATA_OFFSET;
    int MULTI_SLOTS_OFFSET = MULTI_NAME_OFFSET + 32;
    int MULTI_SLOT_LENGTH = 24;

    byte SINGLE_DUMP = 0x10;
    byte MULTI_DUMP = 0x11;

    class BlofeldInitSysexMessage extends SysexMessage {
        BlofeldInitSysexMessage(byte[] data, int length) throws InvalidMidiDataException {
            super(data, length);
        }
    }

    static BlofeldInitSysexMessage loadInitSysEx(String initSysExFileName, int length) {
        try (InputStream initFileStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(initSysExFileName)) {
            if (initFileStream == null) {
                throw new RuntimeException("Cannot load " + initSysExFileName + " from resources");
            }
            byte[] message = new byte[length];
            int read = initFileStream.read(message);
            if (read != length) {
                throw new RuntimeException("Invalid sysex size " + initSysExFileName + " (" + read + "!=" + length + ")");
            }
            return new BlofeldInitSysexMessage(message, message.length);
        } catch (IOException | InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    default boolean isEmpty() {
        return getSysEx() instanceof BlofeldInitSysexMessage;
    }

    @Override
    default Sound cloneForEditBuffer() {
        return clone((byte) 0x7F, (byte) 0x00);
    }
}
