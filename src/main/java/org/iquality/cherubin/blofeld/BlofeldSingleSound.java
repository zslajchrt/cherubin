package org.iquality.cherubin.blofeld;

import org.iquality.cherubin.AbstractSound;
import org.iquality.cherubin.SingleSound;
import org.iquality.cherubin.SoundCategory;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

public class BlofeldSingleSound extends AbstractSound implements BlofeldSoundCommon, SingleSound {

    private static final String INIT_FILE_NAME = "blofeld-init-single.syx";
    private static BlofeldInitSysexMessage INIT_SYSEX;

    public static final int MULTI_SOUND_MSG_LENGTH = 392;

    static {
        INIT_SYSEX = BlofeldSoundCommon.loadInitSysEx(INIT_FILE_NAME, MULTI_SOUND_MSG_LENGTH);
    }

    public BlofeldSingleSound() {
        super(-1, INIT_SYSEX, "", BlofeldFactory.INSTANCE);
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
        return SoundCategory.CATEGORIES[catId];
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
    public BlofeldSingleSound clone(int programBank, int programNumber) {
        try {
            byte[] data = getSysEx().getMessage(); // getMessage() returns a copy
            data[BANK_OFFSET] = (byte) programBank;
            data[PROGRAM_OFFSET] = (byte) programNumber;
            return new BlofeldSingleSound(getId(), isEmpty() ? new BlofeldInitSysexMessage(data, data.length) : new SysexMessage(data, data.length), getSoundSetName());
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }
}
