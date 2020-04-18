package org.iquality.cherubin.peak;

import org.iquality.cherubin.*;

import javax.sound.midi.SysexMessage;
import java.util.Collections;
import java.util.List;

public class PeakFactory implements SynthFactory {

    public static final PeakFactory INSTANCE = new PeakFactory();
    public static final int BANKS_COUNT = 4;
    public static final int BANK_SIZE = 128;
    public static final String SYNTH_ID = "Novation Peak";

    @Override
    public List<Sound> createSounds(SysexMessage sysexMessage, String soundSetName) {
        return Collections.singletonList(createOneSound(-1, "", sysexMessage, SoundCategory.Init, soundSetName));
    }

    @Override
    public Sound createOneSound(int id, String name, SysexMessage sysexMessage, SoundCategory category, String soundSetName) {
        assert accepts(sysexMessage);
        // ignore name and category as they are encoded in sysex
        return new PeakSound(id, sysexMessage, soundSetName);
    }

    @Override
    public SingleSound createSingleSound() {
        return new PeakSound();
    }

    @Override
    public MultiSound createMultiSound() {
        throw new UnsupportedOperationException("No multi sound in Novation Peak");
    }

    @Override
    public boolean accepts(SysexMessage message) {
        byte[] msg = message.getMessage();
        return msg.length % PeakSound.MESSAGE_LENGTH == 0 && msg[0] == (byte) 0xF0 && msg[1] == (byte) 0x00 && msg[2] == (byte) 0x20 && msg[3] == (byte) 0x29;
    }

    @Override
    public boolean isMulti(SysexMessage sysex) {
        return false;
    }

    @Override
    public int getMultiSlotCount() {
        return 0;
    }

    @Override
    public int getBankCount() {
        return BANKS_COUNT;
    }

    @Override
    public int getBankSize() {
        return BANK_SIZE;
    }

    @Override
    public boolean hasMultiBank() {
        return false;
    }

    @Override
    public int getMultiBankSize() {
        return 0;
    }

    @Override
    public String getSynthId() {
        return SYNTH_ID;
    }

    @Override
    public String toString() {
        return getSynthId();
    }
}
