package org.iquality.cherubin.bassStation2;

import org.iquality.cherubin.*;

import javax.sound.midi.SysexMessage;

public class BS2Sound extends AbstractSound implements SingleSound {

    public static final int NAME_OFFSET = 137;
    public static final int NAME_LENGTH = 16;
    public static final int PATCH_NUMBER_OFFSET = 8;
    public static final int MESSAGE_LENGTH = NAME_OFFSET + NAME_LENGTH + 1;

    private static final String INIT_FILE_NAME = "bs2-init.syx";
    private static SysexMessage INIT_SYSEX;

    static {
        INIT_SYSEX = Utils.loadInitSysEx(INIT_FILE_NAME, MESSAGE_LENGTH);
    }

    private SoundCategory category;

    public BS2Sound() {
        this(-1, INIT_SYSEX, SoundCategory.Init, "");
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
    protected int getNameOffset() {
        return NAME_OFFSET;
    }

    @Override
    protected int getNameMaxLength() {
        return NAME_LENGTH;
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
    protected void patch(byte[] data, int programBank, int programNumber) {
        data[PATCH_NUMBER_OFFSET - 1] = 1;
        data[PATCH_NUMBER_OFFSET] = (byte) programNumber;
    }

    @Override
    protected void patchForEditBuffer(byte[] data) {
        data[PATCH_NUMBER_OFFSET - 1] = 0;
        data[PATCH_NUMBER_OFFSET] = 0;
    }
}
