package org.iquality.cherubin;

import org.iquality.cherubin.blofeld.InitSysexMessage;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import java.util.Arrays;
import java.util.function.Function;

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
    }

    protected abstract int getNameOffset();

    protected abstract int getNameMaxLength();

    @Override
    public void setSysEx(SysexMessage sysEx) {
        assert sysEx != null;
        if (!getSynthFactory().accepts(sysEx)) {
            throw new RuntimeException("SysEx not accepted by synth factory " + getSynthFactory());
        }
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
    public void setBank(int bank) {
        setBankImp(bank);
    }

    protected abstract void setBankImp(int bank);

    @Override
    public final int getProgram() {
        return getProgramImp();
    }

    @Override
    public void setProgram(int program) {
        setProgramImp(program);
    }

    protected abstract void setProgramImp(int program);

    @Override
    public void setCategory(SoundCategory category) {
        if (!isInit()) {
            setCategoryImp(category);
        }
    }

    protected abstract void setCategoryImp(SoundCategory category);

    protected abstract int getProgramImp();

    protected SysexMessage patchSysEx(Function<byte[], byte[]> patcher) {
        byte[] data = patcher.apply(getSysEx().getMessage());
        try {
            if (sysEx instanceof InitSysexMessage) {
                return new InitSysexMessage(data, data.length);
            } else {
                return new SysexMessage(data, data.length);
            }
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    protected void updateSysEx(int offset, byte value) {
        setSysEx(patchSysEx(data -> {
            data[offset] = value;
            return data;
        }));
    }

    protected void updateSysEx(Function<byte[], byte[]> patcher) {
        setSysEx(patchSysEx(patcher));
    }

    @Override
    public Sound clone() {
        return patchSound(data -> data);
    }

    @Override
    public Sound cloneForEditBuffer() {
        return patchSound(this::patchForEditBuffer);
    }

    @Override
    public void verify() throws VerificationException {
    }

    private Sound patchSound(Function<byte[], byte[]> patcher) {
        SysexMessage patchedSysEx = patchSysEx(patcher);
        return getSynthFactory().createOneSound(getId(), getName(), patchedSysEx, getCategory(), getSoundSetName());
    }

    protected abstract byte[] patchForEditBuffer(byte[] data);

    protected void updateCheckSum(int startOffset, int endOffset) {
        updateSysEx(data -> {
            data[endOffset] = Utils.checksum(data, startOffset, endOffset);
            return data;
        });
    }

    @Override
    public String toString() {
        //return String.format("%s (%s%d)",  getName(), "" + (char)(getBank() + 'A'), (getProgram() + 1));
        return getName().trim();
    }
}
