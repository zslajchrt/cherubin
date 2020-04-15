package org.iquality.cherubin;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

public abstract class AbstractSound implements Sound {

    private final int id;
    private final SynthFactory synthFactory;
    protected SysexMessage sysEx;
    protected String soundSetName;

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
    public final int getBank() {
        return isEmpty() ? -1 : getBankImp();
    }

    protected abstract int getBankImp();

    @Override
    public final int getProgram() {
        return isEmpty() ? -1 : getProgramImp();
    }

    @Override
    public void setCategory(SoundCategory category) {
        if (!isEmpty()) setCategoryImp(category);
    }

    protected abstract void setCategoryImp(SoundCategory category);

    protected abstract int getProgramImp();

    protected void updateSysEx(int offset, byte value) {
        byte[] data = getSysEx().getMessage();
        data[offset] = value;
        try {
            sysEx = new SysexMessage(data, data.length);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return getName();
    }
}
