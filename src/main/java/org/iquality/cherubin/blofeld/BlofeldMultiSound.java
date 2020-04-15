package org.iquality.cherubin.blofeld;

import org.iquality.cherubin.AbstractSound;
import org.iquality.cherubin.MultiSound;
import org.iquality.cherubin.SoundCategory;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlofeldMultiSound extends AbstractSound implements MultiSound, BlofeldSoundCommon {

    private static final String INIT_FILE_NAME = "blofeld-init-multi.syx";
    private static BlofeldSingleSound.BlofeldInitSysexMessage INIT_SYSEX;

    class BlofeldSlotRef implements SlotRef {
        private final int slotNum;

        BlofeldSlotRef(int slotNum) {
            this.slotNum = slotNum;
        }

        public int getBank() {
            byte[] data = getSysEx().getMessage();
            return data[MULTI_SLOTS_OFFSET + slotNum * MULTI_SLOT_LENGTH];
        }

        public void setRef(int bank, int program) {
            updateSysEx(MULTI_SLOTS_OFFSET + slotNum * MULTI_SLOT_LENGTH, (byte) bank);
            updateSysEx(MULTI_SLOTS_OFFSET + slotNum * MULTI_SLOT_LENGTH + 1, (byte) program);
        }

        public int getProgram() {
            byte[] data = getSysEx().getMessage();
            return data[MULTI_SLOTS_OFFSET + slotNum * MULTI_SLOT_LENGTH + 1];
        }

        @Override
        public String toString() {
            return String.format("%s%s", (char) ('A' + getBank()), getProgram() + 1);
        }
    }

    public static final int MULTI_SOUND_MSG_LENGTH = 425;

    static {
        INIT_SYSEX = BlofeldSoundCommon.loadInitSysEx(INIT_FILE_NAME, MULTI_SOUND_MSG_LENGTH);
    }

    private final List<BlofeldSlotRef> slotRefs = new ArrayList<>();

    private SoundCategory category = SoundCategory.Init;

    public BlofeldMultiSound() {
        super(-1, INIT_SYSEX, "", BlofeldFactory.INSTANCE);
    }

    public BlofeldMultiSound(int id, SysexMessage sysEx, SoundCategory category, String soundSetName) {
        super(id, sysEx, soundSetName, BlofeldFactory.INSTANCE);
        this.category = category;
        for (int i = 0; i < 16; i++ ) {
            slotRefs.add(new BlofeldSlotRef(i));
        }
    }

    @Override
    protected String getNameImp() {
        byte[] msg = getSysEx().getMessage();
        return new String(msg, MULTI_NAME_OFFSET, NAME_LENGTH);
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
        return msg[PROGRAM_OFFSET];
    }

    @Override
    public boolean isEmpty() {
        return getSysEx() instanceof BlofeldSingleSound.BlofeldInitSysexMessage;
    }

    @Override
    public BlofeldMultiSound clone(int programBank, int programNumber) {
        try {
            byte[] data = getSysEx().getMessage(); // getMessage() returns a copy
            data[BANK_OFFSET] = 0; // there is no bank for multi
            data[PROGRAM_OFFSET] = (byte) programNumber;
            return new BlofeldMultiSound(getId(), isEmpty() ? new BlofeldInitSysexMessage(data, data.length) : new SysexMessage(data, data.length), category, getSoundSetName());
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<SlotRef> getSlotRefs() {
        return Collections.unmodifiableList(slotRefs);
    }
}
