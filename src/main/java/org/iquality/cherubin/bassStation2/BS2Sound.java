package org.iquality.cherubin.bassStation2;

import org.iquality.cherubin.AbstractSound;
import org.iquality.cherubin.Sound;
import org.iquality.cherubin.SoundCategory;
import org.iquality.cherubin.SynthFactory;

import javax.sound.midi.SysexMessage;

public class BS2Sound extends AbstractSound {
    public BS2Sound(int id, SysexMessage sysEx, String soundSetName, SynthFactory synthFactory) {
        super(id, sysEx, soundSetName, synthFactory);
    }

    @Override
    protected String getNameImp() {
        return null;
    }

    @Override
    protected SoundCategory getCategoryImp() {
        return null;
    }

    @Override
    protected byte getBankImp() {
        return 0;
    }

    @Override
    protected byte getProgramImp() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Sound clone(byte programBank, byte programNumber) {
        return null;
    }

    @Override
    public Sound cloneForEditBuffer() {
        return null;
    }
}
