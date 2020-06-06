package org.iquality.cherubin;

import org.iquality.cherubin.blofeld.InitSysexMessage;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

public abstract class AbstractSound implements Sound {

    private int id;
    private final SynthFactory synthFactory;
    protected SysexMessage sysEx;
    protected String soundSetName;
    private final Map<Object, Object> customData = new HashMap<>();

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
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public final String getName() {
        return isInit() ? "Empty" : getNameImp();
    }

    protected String getNameImp() {
        byte[] msg = getSysEx().getMessage();
        return new String(msg, getNameOffset(), getNameMaxLength()).trim();
    }

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

    @Override
    public void appendToName(String ext) {
        String newName = getName();
        int toCut = newName.length() + ext.length() - getNameMaxLength();
        if (toCut > 0) {
            newName = newName.substring(0, newName.length() - toCut);
        }
        newName += ext;
        setName(newName);
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
    public void setCustomData(Object name, Object data) {
        customData.put(name, data);
    }

    @Override
    public Object getCustomData(Object name) {
        return customData.get(name);
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
        return copyCustomData(getSynthFactory().createOneSound(getId(), getName(), patchedSysEx, getCategory(), getSoundSetName()));
    }

    private Sound copyCustomData(Sound sound) {
        for (Iterator<Map.Entry<Object, Object>> iterator = customData.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Object, Object> next = iterator.next();
            sound.setCustomData(next.getKey(), next.getValue());
        }
        return sound;
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
