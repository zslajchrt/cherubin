package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;
import java.util.ArrayList;
import java.util.List;

public interface SynthFactory {

    List<Sound> createSounds(SysexMessage sysexMessage, String soundSetName);

    Sound createSingleSound(int id, String name, SysexMessage sysexMessage, SoundCategory category, String soundSetName);

    SingleSound createSingleSound();

    MultiSound createMultiSound();

    default List<Sound> createBank(int bankNum) {
        List<Sound> bankList = new ArrayList<>();
        for (int program = 0; program < getBankSize(); program++) {
            bankList.add(createSingleSound().clone(bankNum, program));
        }
        return bankList;
    }

    default List<MultiSound> createMultiBank() {
        List<MultiSound> bankList = new ArrayList<>();
        for (int program = 0; program < getMultiBankSize(); program++) {
            bankList.add(createMultiSound().clone(0, program));
        }
        return bankList;
    }

    int getBankCount();

    int getBankSize();

    boolean hasMultiBank();

    int getMultiBankSize();

    String getSynthId();

    boolean accepts(SysexMessage message);

    boolean isMulti(SysexMessage sysex);

    int getMultiSlotCount();
}
