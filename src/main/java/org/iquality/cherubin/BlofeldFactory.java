package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;
import java.util.ArrayList;
import java.util.List;

public class BlofeldFactory implements SynthFactory {

    public static final int BANKS_COUNT = 8;
    public static final int BANK_SIZE = 128;
    public static final SynthFactory INSTANCE = new BlofeldFactory();

    @Override
    public Sound createSound(int id, SysexMessage sysexMessage, String soundSetName) {
        assert accepts(sysexMessage);
        if (isMulti(sysexMessage)) {
            return new BlofeldMultiSound(id, sysexMessage, soundSetName);
        } else {
            return new BlofeldSingleSound(id, sysexMessage, soundSetName);
        }
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
    public List<Sound> createBank(int bankNum) {
        List<Sound> bankList = new ArrayList<>();
        for (int program = 0; program < BANK_SIZE; program++) {
            bankList.add(new BlofeldSingleSound().clone((byte) bankNum, (byte) program));
        }
        return bankList;
    }

    @Override
    public List<MultiSound> createMulti() {
        List<MultiSound> bankList = new ArrayList<>();
        for (int program = 0; program < BANK_SIZE; program++) {
            bankList.add(new BlofeldMultiSound().clone((byte)0, (byte) program));
        }
        return bankList;
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
    public int getMultiBankSize() {
        return BANK_SIZE;
    }

    @Override
    public String getSynthId() {
        return "Blofeld";
    }

    @Override
    public String getInitialName() {
        return "INIT";
    }

    @Override
    public String toString() {
        return getSynthId();
    }
}
