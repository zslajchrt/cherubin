package org.iquality.cherubin;

import com.sun.org.apache.xpath.internal.operations.Mult;

import javax.sound.midi.SysexMessage;
import java.util.ArrayList;
import java.util.List;

public interface SynthFactory {

    List<Sound> createSounds(SysexMessage sysexMessage, String soundSetName);

    Sound createOneSound(int id, String name, SysexMessage sysexMessage, SoundCategory category, String soundSetName);

    SingleSound createSingleSound();

    MultiSound createMultiSound();

    default List<Sound> createBank(int bankNum) {
        List<Sound> bankList = new ArrayList<>();
        for (int program = 0; program < getBankSize(); program++) {
            Sound clone = createSingleSound().clone(bankNum, program);
            bankList.add(clone);
        }
        return bankList;
    }

    default List<MultiSound> createMultiBank() {
        List<MultiSound> bankList = new ArrayList<>();
        for (int program = 0; program < getMultiBankSize(); program++) {
            bankList.add((MultiSound) createMultiSound().clone(0, program));
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
