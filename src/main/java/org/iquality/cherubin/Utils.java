package org.iquality.cherubin;

import org.iquality.cherubin.blofeld.InitSysexMessage;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import java.io.IOException;
import java.io.InputStream;

public class Utils {

    public static InitSysexMessage loadInitSysEx(String initSysExFileName, int length) {
        try (InputStream initFileStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(initSysExFileName)) {
            if (initFileStream == null) {
                throw new RuntimeException("Cannot load " + initSysExFileName + " from resources");
            }
            byte[] message = new byte[length];
            int read = initFileStream.read(message);
            if (read != length) {
                throw new RuntimeException("Invalid sysex size " + initSysExFileName + " (" + read + "!=" + length + ")");
            }
            return new InitSysexMessage(message, message.length);
        } catch (IOException | InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends MidiMessage> T printSysExDump(T message) {
        if (false) {
            return message;
        }

        byte[] msg = message.getMessage();

        for (int i = 0; i < msg.length; i++) {
            System.out.printf("%02X ", i);
        }

        System.out.println();

        for (int i = 0; i < msg.length; i++) {
            byte b = msg[i];
            System.out.printf("%02X ", b);
        }

        System.out.println();

        return message;
    }


}
