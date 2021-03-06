package org.iquality.cherubin.blofeld;

import org.iquality.cherubin.*;

import javax.sound.midi.SysexMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlofeldMultiSound extends AbstractSound implements MultiSound, BlofeldSoundCommon {

    private static final String INIT_FILE_NAME = "blofeld-init-multi.syx";
    private static InitSysexMessage INIT_SYSEX;

    class BlofeldSoundSlotRef implements SoundSlotRef {
        private final int slotNum;

        BlofeldSoundSlotRef(int slotNum) {
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
        INIT_SYSEX = Utils.loadInitSysEx(INIT_FILE_NAME, MULTI_SOUND_MSG_LENGTH);
    }

    private final List<BlofeldSoundSlotRef> slotRefs = new ArrayList<>();

    private SoundCategory category;

    public BlofeldMultiSound() {
        this(-1, INIT_SYSEX, SoundCategory.Multi, "");
    }

    public BlofeldMultiSound(int id, SysexMessage sysEx, SoundCategory category, String soundSetName) {
        super(id, sysEx, soundSetName, BlofeldFactory.INSTANCE);
        this.category = category;
        for (int i = 0; i < 16; i++) {
            slotRefs.add(new BlofeldSoundSlotRef(i));
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        updateCheckSum(MULTI_NAME_OFFSET, MULTI_CHECKSUM_OFFSET);
    }

    @Override
    protected int getNameOffset() {
        return MULTI_NAME_OFFSET;
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
        return 0; // there is no bank for multi
    }

    @Override
    protected int getProgramImp() {
        byte[] msg = getSysEx().getMessage();
        return msg[PROGRAM_OFFSET];
    }

    @Override
    protected void setBankImp(int bank) {
        // no banks in Blofeld multi mode
    }

    @Override
    protected void setProgramImp(int program) {
        updateSysEx(PROGRAM_OFFSET, (byte) program);
    }

    @Override
    protected byte[] patchForEditBuffer(byte[] data) {
        //patch(data, 0x7F, 0x00);
        data[BANK_OFFSET] = (byte) 07f;
        data[PROGRAM_OFFSET] = (byte) 0x00;
        data[MULTI_CHECKSUM_OFFSET] = Utils.checksum(data, MULTI_NAME_OFFSET, MULTI_CHECKSUM_OFFSET);
        return data;
    }

    @Override
    public List<SoundSlotRef> getSlotRefs() {
        return Collections.unmodifiableList(slotRefs);
    }

    @Override
    public void verify() throws VerificationException {
        byte[] msg = getSysEx().getMessage();
        byte checksum = Utils.checksum(msg, MULTI_NAME_OFFSET, MULTI_CHECKSUM_OFFSET);
        byte currentChecksum = msg[MULTI_CHECKSUM_OFFSET];
        if (checksum != currentChecksum) {
            throw new VerificationException("Inconsistent checksum");
        }
    }
}
