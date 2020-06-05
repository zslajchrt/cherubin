package org.iquality.cherubin.blofeld;

import org.iquality.cherubin.*;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import java.util.Arrays;

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
    public void initialize() {
        super.initialize();
        updateCheckSum(SDATA_OFFSET, SINGLE_CHECKSUM_OFFSET);
    }

    @Override
    protected String getNameImp() {
        byte[] msg = getSysEx().getMessage();
        return new String(msg, SINGLE_NAME_OFFSET, NAME_LENGTH);
    }

    @Override
    protected int getNameOffset() {
        return SINGLE_NAME_OFFSET;
    }

    @Override
    protected int getNameMaxLength() {
        return NAME_LENGTH;
    }

    @Override
    protected SoundCategory getCategoryImp() {
        byte[] msg = getSysEx().getMessage();
        int catId = msg[SINGLE_CAT_OFFSET];
        return SoundCategory.values()[catId];
    }

    @Override
    protected void setCategoryImp(SoundCategory category) {
        updateSysEx(SINGLE_CAT_OFFSET, (byte) category.ordinal());
        updateCheckSum(SDATA_OFFSET, SINGLE_CHECKSUM_OFFSET);
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
    protected void setBankImp(int bank) {
        updateSysEx(BANK_OFFSET, (byte) bank);
    }

    @Override
    protected void setProgramImp(int program) {
        updateSysEx(PROGRAM_OFFSET, (byte) program);
    }

    @Override
    protected byte[] patchForEditBuffer(byte[] data) {
        data[BANK_OFFSET] = 0x7F;
        data[PROGRAM_OFFSET] = 0x00;
        data[SINGLE_CHECKSUM_OFFSET] = Utils.checksum(data, SDATA_OFFSET, SINGLE_CHECKSUM_OFFSET);
        return data;
    }
}
