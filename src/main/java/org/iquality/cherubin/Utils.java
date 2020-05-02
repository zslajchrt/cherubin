package org.iquality.cherubin;

import org.iquality.cherubin.blofeld.InitSysexMessage;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.iquality.cherubin.blofeld.BlofeldSoundCommon.MULTI_CHECKSUM_OFFSET;
import static org.iquality.cherubin.blofeld.BlofeldSoundCommon.MULTI_NAME_OFFSET;

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

    public static byte checksum(byte[] data, int startInc, int endExc) {
        byte acc = 0;
        for (int i = startInc; i < endExc; i++) {
            acc += data[i];
        }
        return (byte) (acc & (byte) 127);
    }

    public static void main(String[] args) {
        //int expected = 0x2f;
        //String input = "F0 3E 13 00 11 00 00 4D 79 4D 75 6C 74 69 31 20 20 20 20 20 20 20 20 00 7F 37 00 00 00 00 00 00 00 00 00 00 00 00 00 00 14 64 40 00 40 40 02 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 02 39 64 40 00 40 40 03 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 04 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 05 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 06 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 07 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 08 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 09 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 0A 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 0B 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 0C 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 0D 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 0E 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 0F 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 10 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 11 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 2F F7";
        int expected = 0x1e;
        String input = "F0 3E 13 00 11 00 00 4D 79 4D 75 6C 74 69 31 20 20 20 20 20 20 20 20 00 7F 37 00 00 00 00 00 00 00 00 00 00 00 00 00 02 47 64 65 00 40 40 02 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 39 2C 40 00 40 40 03 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 04 4B 64 40 00 40 40 04 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 05 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 06 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 07 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 08 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 09 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 0A 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 0B 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 0C 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 0D 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 0E 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 0F 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 10 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 00 00 64 40 00 40 40 11 00 7F 01 7F 07 3F 00 00 00 00 00 00 00 00 00 00 1E F7";
        String[] s = input.split(" ");
        byte[] data = new byte[s.length];

        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) Integer.parseInt(s[i], 16);
        }

        byte chksum = checksum(data, MULTI_NAME_OFFSET, MULTI_CHECKSUM_OFFSET);
        System.out.printf("%02X\n", chksum);

        System.exit(0);
        for (int i = 0; i <= 0x1A6; i++) {
            for (int j = 0; j <= i; j++) {
                byte chk = checksum(data, j, i);
                if ((int) chk == expected) {
                    System.out.printf("%d:%d\n", j, i);
                }
            }
        }
    }
}
