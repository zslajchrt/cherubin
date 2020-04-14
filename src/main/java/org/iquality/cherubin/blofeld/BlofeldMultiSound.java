package org.iquality.cherubin.blofeld;

import org.iquality.cherubin.AbstractSound;
import org.iquality.cherubin.MultiSound;
import org.iquality.cherubin.SoundCategory;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

public class BlofeldMultiSound extends AbstractSound implements MultiSound, BlofeldSoundCommon {

    private static final String INIT_FILE_NAME = "blofeld-init-multi.syx";
    private static BlofeldSingleSound.BlofeldInitSysexMessage INIT_SYSEX;

    static {
        INIT_SYSEX = BlofeldSoundCommon.loadInitSysEx(INIT_FILE_NAME, 425);
    }

    public BlofeldMultiSound() {
        super(-1, INIT_SYSEX, "", BlofeldFactory.INSTANCE);
    }

    public BlofeldMultiSound(int id, SysexMessage sysEx, String soundSetName) {
        super(id, sysEx, soundSetName, BlofeldFactory.INSTANCE);
    }

    @Override
    protected String getNameImp() {
        byte[] msg = getSysEx().getMessage();
        return new String(msg, MULTI_NAME_OFFSET, NAME_LENGTH);
    }

    @Override
    protected SoundCategory getCategoryImp() {
        return SoundCategory.Init;
    }

    @Override
    protected byte getBankImp() {
        return 0;
    }

    @Override
    protected byte getProgramImp() {
        byte[] msg = getSysEx().getMessage();
        return msg[PROGRAM_OFFSET];
    }

    @Override
    public boolean isEmpty() {
        return getSysEx() instanceof BlofeldSingleSound.BlofeldInitSysexMessage;
    }

    @Override
    public BlofeldMultiSound clone(byte programBank, byte programNumber) {
        try {
            byte[] data = getSysEx().getMessage(); // getMessage() returns a copy
            data[BANK_OFFSET] = 0; // there is no bank for multi
            data[PROGRAM_OFFSET] = programNumber;
            return new BlofeldMultiSound(getId(), isEmpty() ? new BlofeldInitSysexMessage(data, data.length) : new SysexMessage(data, data.length), getSoundSetName());
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SlotRef[] getSlotRefs() {
        byte[] data = getSysEx().getMessage();
        SlotRef[] slotRefs = new SlotRef[16];

        for (int slotNum = 0; slotNum < slotRefs.length; slotNum++) {
            byte bank = data[MULTI_SLOTS_OFFSET + slotNum * MULTI_SLOT_LENGTH];
            byte prg = data[MULTI_SLOTS_OFFSET + slotNum * MULTI_SLOT_LENGTH + 1];
            slotRefs[slotNum] = new SlotRef(bank, prg);
        }

        return slotRefs;
    }
}
