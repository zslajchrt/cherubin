package org.iquality.cherubin;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;

public abstract class SoundDumpReceiver implements Receiver {

    public SoundDumpReceiver() {
    }

    protected abstract void onSingleSoundDump(SysexMessage sysEx);

    protected abstract void onMultiSoundDump(SysexMessage sysEx);

    public void send(MidiMessage message, long timeStamp) {
        if (message instanceof SysexMessage ) {
            SysexMessage sysex = (SysexMessage) message;
            SynthFactory synthFactory = SynthFactoryRegistry.INSTANCE.getSynthFactory(sysex);
            if (synthFactory != null) {
                if (synthFactory.isMulti(sysex)) {
                    onMultiSoundDump(sysex);
                } else {
                    // assume the message is a single sound
                    onSingleSoundDump(sysex);
                }
            }
        }
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
