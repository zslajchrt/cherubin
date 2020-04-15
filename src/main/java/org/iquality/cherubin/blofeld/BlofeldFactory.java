package org.iquality.cherubin.blofeld;

import org.iquality.cherubin.*;

import javax.sound.midi.SysexMessage;
import java.util.Collections;
import java.util.List;

public class BlofeldFactory implements SynthFactory {

    public static final int BANKS_COUNT = 8;
    public static final int BANK_SIZE = 128;

    public static final SynthFactory INSTANCE = new BlofeldFactory();

    @Override
    public List<Sound> createSounds(SysexMessage sysexMessage, String soundSetName) {
        return Collections.singletonList(createSingleSound(-1, "", sysexMessage, SoundCategory.Init, soundSetName));
    }

    @Override
    public Sound createSingleSound(int id, String name, SysexMessage sysexMessage, SoundCategory category, String soundSetName) {
        assert accepts(sysexMessage);
        if (isMulti(sysexMessage)) {
            return new BlofeldMultiSound(id, sysexMessage, category, soundSetName);
        } else {
            // ignore name and category as they are encoded in sysex
            return new BlofeldSingleSound(id, sysexMessage, soundSetName);
        }
    }

    @Override
    public SingleSound createSingleSound() {
        return new BlofeldSingleSound();
    }

    @Override
    public MultiSound createMultiSound() {
        return new BlofeldMultiSound();
    }

    @Override
    public boolean accepts(SysexMessage message) {
        byte[] msg = message.getMessage();
        return !(msg.length < BlofeldSoundCommon.SDATA_OFFSET || !(msg[0] == (byte) 0xF0 && msg[1] == (byte) 0x3E && msg[2] == (byte) 0x13));
    }

    @Override
    public boolean isMulti(SysexMessage sysex) {
        byte[] msg = sysex.getMessage();
        byte msgId = msg[4];

        switch (msgId) {
            case BlofeldSoundCommon.MULTI_DUMP:
                return true;
            case BlofeldSoundCommon.SINGLE_DUMP:
                return false;
            default:
                throw new RuntimeException("Unknown Blofeld sound type");
        }
    }

    @Override
    public int getMultiSlotCount() {
        return 16;
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
        return true;
    }

    @Override
    public int getMultiBankSize() {
        return BANK_SIZE;
    }

    @Override
    public String getSynthId() {
        return "Blofeld";
    }

    @Override
    public String toString() {
        return getSynthId();
    }
}
