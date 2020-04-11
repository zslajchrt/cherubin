package org.iquality.cherubin;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;

public abstract class BlofeldDumpReceiver implements Receiver {

    static final int MESSAGE_ID_OFFSET = 4;
    static final int PROGRAM_BANK_OFFSET = 5;
    static final int PROGRAM_NUMBER_OFFSET = 6;
    static final int SDATA_OFFSET = 7;
    static final int SDATA_LENGTH = 380;
    static final int SINGLE_NAME_OFFSET = 363;
    static final int MULTI_NAME_OFFSET = 0;
    static final int NAME_LENGTH = 16;
    static final int SINGLE_CAT_OFFSET = 379;

    static final byte SINGLE_DUMP = 0x10;
    static final byte MULTI_DUMP = 0x11;

    protected abstract void onSingleSoundDump(String soundName, SoundCategory category, SysexMessage sysEx);

    protected abstract void onMultiSoundDump(String soundName, SysexMessage sysEx);

    public void send(MidiMessage message, long timeStamp) {

        byte[] msg = message.getMessage();
        if (!(message instanceof SysexMessage) || (msg.length < SDATA_OFFSET) || !(msg[0] == (byte) 0xF0 && msg[1] == (byte) 0x3E && msg[2] == (byte) 0x13)) {
            return;
        }

        //printSysExDump(message, timeStamp);
        System.out.print(".");

        byte msgId = msg[4];
        switch (msgId) {
            case SINGLE_DUMP:
                handleSingle((SysexMessage) message);
                break;
            case MULTI_DUMP:
                handleMulti((SysexMessage) message);
                break;
            default:
                break;
        }
    }

    private void handleSingle(SysexMessage message) {
        byte[] msg = message.getMessage();
        String soundName = new String(msg, SDATA_OFFSET + SINGLE_NAME_OFFSET, NAME_LENGTH);
        int catId = msg[SDATA_OFFSET + SINGLE_CAT_OFFSET];
        SoundCategory category = SoundCategory.CATEGORIES[catId];

        onSingleSoundDump(soundName, category, message);
    }

    private void handleMulti(SysexMessage message) {
        byte[] msg = message.getMessage();
        String soundName = new String(msg, SDATA_OFFSET + MULTI_NAME_OFFSET, NAME_LENGTH);
        onMultiSoundDump(soundName, message);
    }

    private void printSysExDump(MidiMessage message, long timeStamp) {
        byte[] msg = message.getMessage();

        for (int i = 0; i < msg.length; i++) {
            byte b = msg[i];
            System.out.printf("%02X ", b);
        }

        System.out.println();
    }

    public void close() {

    }

}
