package org.iquality.cherubin.blofeld;

import org.iquality.cherubin.*;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

public class BlofeldSingleSound extends AbstractSound implements BlofeldSoundCommon, SingleSound {

    private static final String INIT_FILE_NAME = "blofeld-init-single.syx";
    private static InitSysexMessage INIT_SYSEX;

    public static final int MULTI_SOUND_MSG_LENGTH = 392;

    static {
        INIT_SYSEX = Utils.loadInitSysEx(INIT_FILE_NAME, MULTI_SOUND_MSG_LENGTH);
    }

    public BlofeldSingleSound() {
        this(-1, INIT_SYSEX, "");
    }

    public BlofeldSingleSound(int id, SysexMessage sysEx, String soundSetName) {
        super(id, sysEx, soundSetName, BlofeldFactory.INSTANCE);
    }

    @Override
    protected String getNameImp() {
        byte[] msg = getSysEx().getMessage();
        return new String(msg, SDATA_OFFSET + SINGLE_NAME_OFFSET, NAME_LENGTH);
    }

    @Override
    protected SoundCategory getCategoryImp() {
        byte[] msg = getSysEx().getMessage();
        int catId = msg[SDATA_OFFSET + SINGLE_CAT_OFFSET];
        return SoundCategory.values()[catId];
    }

    @Override
    protected void setCategoryImp(SoundCategory category) {
        byte[] msg = getSysEx().getMessage();
        msg[SDATA_OFFSET + SINGLE_CAT_OFFSET] = (byte) category.ordinal();
        try {
            sysEx = new SysexMessage(msg, msg.length);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected int getBankImp() {
        byte[] msg = getSysEx().getMessage();
        return msg[BANK_OFFSET];
    }

    @Override
    protected int getProgramImp() {
        byte[] msg = getSysEx().getMessage();
        return msg[PROGRAM_OFFSET];
    }

    @Override
    protected void patch(byte[] data, int programBank, int programNumber) {
        data[BANK_OFFSET] = (byte) programBank;
        data[PROGRAM_OFFSET] = (byte) programNumber;
    }

    @Override
    protected void patchForEditBuffer(byte[] data) {
        patch(data, 0x7F, 0x00);
    }
}
