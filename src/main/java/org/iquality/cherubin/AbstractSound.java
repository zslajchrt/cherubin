package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;

public abstract class AbstractSound implements Sound {

    private final int id;
    private final SynthFactory synthFactory;
    private SysexMessage sysEx;
    private String soundSetName;

    public AbstractSound(int id, SysexMessage sysEx, String soundSetName, SynthFactory synthFactory) {
        assert sysEx != null;
        this.id = id;
        this.sysEx = sysEx;
        this.soundSetName = soundSetName;
        this.synthFactory = synthFactory;
    }

    @Override
    public SynthFactory getSynthFactory() {
        return synthFactory;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public final String getName() {
        return isEmpty() ? "Empty" : getNameImp();
    }

    protected abstract String getNameImp();

    @Override
    public void setSysEx(SysexMessage sysEx) {
        assert sysEx != null;
        this.sysEx = sysEx;
    }

    public void setSoundSetName(String soundSetName) {
        this.soundSetName = soundSetName;
    }

    @Override
    public SysexMessage getSysEx() {
        return sysEx;
    }

    @Override
    public String getSoundSetName() {
        return soundSetName;
    }

    @Override
    public final SoundCategory getCategory() {
        return isEmpty() ? SoundCategory.Init : getCategoryImp();
    }

    protected abstract SoundCategory getCategoryImp();

    @Override
    public final byte getBank() {
        return isEmpty() ? -1 : getBankImp();
    }

    protected abstract byte getBankImp();

    @Override
    public final byte getProgram() {
        return isEmpty() ? -1 : getProgramImp();
    }

    protected abstract byte getProgramImp();

    @Override
    public String toString() {
        return getName();
    }
}
