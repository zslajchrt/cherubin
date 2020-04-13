package org.iquality.cherubin;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

public class BlofeldSingleSound extends AbstractSound implements BlofeldSoundCommon {

    private static final String INIT_FILE_NAME = "blofeld-init-single.syx";
    private static BlofeldInitSysexMessage INIT_SYSEX;

    static {
        INIT_SYSEX = BlofeldSoundCommon.loadInitSysEx(INIT_FILE_NAME, 392);
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
    protected byte getBankImp() {
        byte[] msg = getSysEx().getMessage();
        return msg[BANK_OFFSET];
    }

    @Override
    protected byte getProgramImp() {
        byte[] msg = getSysEx().getMessage();
        return msg[PROGRAM_OFFSET];
    }

    @Override
    public BlofeldSingleSound clone(byte programBank, byte programNumber) {
        try {
            byte[] data = getSysEx().getMessage(); // getMessage() returns a copy
            data[BANK_OFFSET] = programBank;
            data[PROGRAM_OFFSET] = programNumber;
            return new BlofeldSingleSound(getId(), isEmpty() ? new BlofeldInitSysexMessage(data, data.length) : new SysexMessage(data, data.length), getSoundSetName());
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }
}
