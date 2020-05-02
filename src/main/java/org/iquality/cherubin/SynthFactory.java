package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;
import java.util.ArrayList;
import java.util.List;

public abstract class SynthFactory {

    public abstract List<Sound> createSounds(SysexMessage sysexMessage, String soundSetName);

    protected abstract Sound createOneSoundInternal(int id, String name, SysexMessage sysexMessage, SoundCategory category, String soundSetName);

    public final Sound createOneSound(int id, String name, SysexMessage sysexMessage, SoundCategory category, String soundSetName) {
        Sound sound = createOneSoundInternal(id, name, sysexMessage, category, soundSetName);
        sound.initialize();
        return sound;
    }

    protected abstract SingleSound createSingleSoundInternal();

    public final SingleSound createSingleSound() {
        SingleSound sound = createSingleSoundInternal();
        sound.initialize();
        return sound;
    }

    protected abstract MultiSound createMultiSoundInternal();

    public final MultiSound createMultiSound() {
        MultiSound sound = createMultiSoundInternal();
        sound.initialize();
        return sound;
    }

    public List<Sound> createBank(int bankNum) {
        List<Sound> bankList = new ArrayList<>();
        for (int program = 0; program < getBankSize(); program++) {
            Sound clone = createSingleSound().clone(bankNum, program);
            bankList.add(clone);
        }
        return bankList;
    }

    public List<MultiSound> createMultiBank() {
        List<MultiSound> bankList = new ArrayList<>();
        for (int program = 0; program < getMultiBankSize(); program++) {
            bankList.add((MultiSound) createMultiSound().clone(0, program));
        }
        return bankList;
    }

    public abstract int getBankCount();

    public abstract int getBankSize();

    public abstract boolean hasMultiBank();

    public abstract int getMultiBankSize();

    public abstract String getSynthId();

    public abstract boolean accepts(SysexMessage message);

    public abstract boolean isMulti(SysexMessage sysex);

    public abstract int getMultiSlotCount();
}
