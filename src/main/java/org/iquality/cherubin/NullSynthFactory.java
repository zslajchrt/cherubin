package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class NullSynthFactory implements SynthFactory {

    public static final NullSynthFactory INSTANCE = new NullSynthFactory();

    @Override
    public List<Sound> createSounds(SysexMessage sysexMessage, String soundSetName) {
        return Collections.emptyList();
    }

    @Override
    public Sound createSingleSound(int id, String name, SysexMessage sysexMessage, SoundCategory category, String soundSetName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SingleSound createSingleSound() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MultiSound createMultiSound() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBankCount() {
        return 0;
    }

    @Override
    public int getBankSize() {
        return 0;
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
        return "NullSynth";
    }

    @Override
    public boolean accepts(SysexMessage message) {
        return false;
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
    public String toString() {
        return "Null Synth";
    }
}
