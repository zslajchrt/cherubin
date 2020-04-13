package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;
import java.util.List;

public interface SynthFactory {

    Sound createSound(int id, SysexMessage sysexMessage, String soundSetName);

    List<Sound> createBank(int bankNum);

    List<MultiSound> createMulti();

    int getBankCount();

    int getBankSize();

    int getMultiBankSize();

    String getSynthId();

    String getInitialName();

    default boolean isInitial(Synth synth) {
        return getInitialName().equals(synth.getName());
    };

    boolean accepts(SysexMessage message);

    boolean isMulti(SysexMessage sysex);
}
