package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;

public class Utils {

    public static void printSysExDump(SysexMessage message) {
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
    }


}
