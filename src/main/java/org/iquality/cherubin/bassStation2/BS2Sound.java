package org.iquality.cherubin.bassStation2;

import org.iquality.cherubin.*;
import org.iquality.cherubin.blofeld.BlofeldSoundCommon;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

public class BS2Sound extends AbstractSound implements SingleSound {

    public static final int NAME_OFFSET = 137;
    public static final int NAME_LENGTH = 16;
    public static final int PATCH_NUMBER_OFFSET = 8;
    public static final int MESSAGE_LENGTH = NAME_OFFSET + NAME_LENGTH + 1;

    private static final String INIT_FILE_NAME = "bs2-init.syx";
    private static SysexMessage INIT_SYSEX;

    static {
        INIT_SYSEX = BlofeldSoundCommon.loadInitSysEx(INIT_FILE_NAME, MESSAGE_LENGTH);
    }

    private SoundCategory category;

    public BS2Sound() {
        super(-1, INIT_SYSEX, "", BS2Factory.INSTANCE);
        category = SoundCategory.Init;
    }

    public BS2Sound(int id, SysexMessage sysEx, SoundCategory category, String soundSetName) {
        super(id, sysEx, soundSetName, BS2Factory.INSTANCE);
        this.category = category;
    }

    @Override
    protected String getNameImp() {
        byte[] msg = getSysEx().getMessage();
        return new String(msg, NAME_OFFSET, NAME_LENGTH);
    }

    @Override
    protected SoundCategory getCategoryImp() {
        return category;
    }

    @Override
    protected void setCategoryImp(SoundCategory category) {
        this.category = category;
    }

    @Override
    protected int getBankImp() {
        return 0;
    }

    @Override
    protected int getProgramImp() {
        byte[] msg = getSysEx().getMessage();
        return msg[PATCH_NUMBER_OFFSET];
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Sound clone(int programBank, int programNumber) {
        try {
            byte[] data = getSysEx().getMessage(); // getMessage() returns a copy
            data[PATCH_NUMBER_OFFSET - 1] = 0;
            data[PATCH_NUMBER_OFFSET] = (byte) programNumber;
            SysexMessage sysEx = new SysexMessage(data, data.length);
            return new BS2Sound(getId(), sysEx, category, getSoundSetName());
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Sound cloneForEditBuffer() {
        return clone(0, 0);
    }
}
