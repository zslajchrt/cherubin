package org.iquality.cherubin;

import org.iquality.cherubin.blofeld.InitSysexMessage;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import java.util.Arrays;
import java.util.function.Consumer;

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
    public void initialize() {
        repatch();
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
        return isInit() ? "Empty" : getNameImp();
    }

    protected abstract String getNameImp();

    @Override
    public void setName(String name) {
        int nameOffset = getNameOffset();
        int nameMaxLength = getNameMaxLength();
        if (nameOffset < 0 || nameMaxLength <= 0) {
            return;
        }

        byte[] msg = getSysEx().getMessage();
        Arrays.fill(msg, nameOffset, nameOffset + nameMaxLength, (byte) 0x20);
        System.arraycopy(name.getBytes(), 0, msg, nameOffset, Math.min(name.length(), nameMaxLength));
        try {
            sysEx = new SysexMessage(msg, msg.length);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }

        repatch();
    }

    protected abstract int getNameOffset();

    protected abstract int getNameMaxLength();

    @Override
    public void setSysEx(SysexMessage sysEx) {
        assert sysEx != null;
        byte[] data = sysEx.getMessage();
        patch(data, getBank(), getProgram());
        try {
            this.sysEx = new SysexMessage(data, data.length);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
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
    public boolean isInit() {
        return getSysEx() instanceof InitSysexMessage;
    }

    @Override
    public final SoundCategory getCategory() {
        return isInit() ? SoundCategory.Init : getCategoryImp();
    }

    protected abstract SoundCategory getCategoryImp();

    @Override
    public final int getBank() {
        return getBankImp();
    }

    protected abstract int getBankImp();

    @Override
    public final int getProgram() {
        return getProgramImp();
    }

    @Override
    public void setCategory(SoundCategory category) {
        if (!isInit()) {
            setCategoryImp(category);
            repatch();
        }
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
    public Sound clone(int programBank, int programNumber) {
        return cloneHelper(data -> patch(data, programBank, programNumber));
    }

    @Override
    public Sound cloneForEditBuffer() {
        return cloneHelper(this::patchForEditBuffer);
    }

    @Override
    public void verify() throws VerificationException {
    }

    private Sound cloneHelper(Consumer<byte[]> patcher) {
        SysexMessage sysEx = getSysEx();
        byte[] data = sysEx.getMessage(); // getMessage() returns a copy
        patcher.accept(data);
        try {
            if (sysEx instanceof InitSysexMessage) {
                sysEx = new InitSysexMessage(data, data.length);
            } else {
                sysEx = new SysexMessage(data, data.length);
            }
            return getSynthFactory().createOneSound(getId(), getName(), sysEx, getCategory(), getSoundSetName());
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    protected void repatch() {
        if (sysEx instanceof InitSysexMessage) {
            return;
        }

        byte[] msg = sysEx.getMessage();
        patch(msg, getBankImp(), getProgramImp());
        try {
            sysEx = new SysexMessage(msg, msg.length);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void patch(byte[] data, int programBank, int programNumber);

    protected abstract void patchForEditBuffer(byte[] data);

    @Override
    public String toString() {
        //return String.format("%s (%s%d)",  getName(), "" + (char)(getBank() + 'A'), (getProgram() + 1));
        return getName().trim();
    }
}
