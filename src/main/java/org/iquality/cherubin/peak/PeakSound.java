package org.iquality.cherubin.peak;

import org.iquality.cherubin.AbstractSound;
import org.iquality.cherubin.SingleSound;
import org.iquality.cherubin.SoundCategory;
import org.iquality.cherubin.Utils;
import org.iquality.cherubin.blofeld.InitSysexMessage;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

public class PeakSound extends AbstractSound implements SingleSound {
    public static final int NAME_OFFSET = 0x10;
    public static final int NAME_LENGTH = 16;
    public static final int BANK_OFFSET = 0xC;
    /**
     * When cloning a Peak sound for the edit buffer, the byte must be cleared at this offset. Otherwise it is 1.
     */
    public static final int REGULAR_PATCH_FLAG_OFFSET = 0x8;
    public static final int PROGRAM_OFFSET = 0xD;
    public static final int CATEGORY_OFFSET = 0x20;
    public static final int MESSAGE_LENGTH = 527;

    public enum PeakCategory {
        All,
        Arp,
        Bass,
        Bell,
        Classic,
        Drum,
        Keys,
        Lead,
        Motion,
        Pad,
        Poly,
        SFX,
        Strings,
        User1,
        User2;

        final SoundCategory generalCategory;

        PeakCategory() {
            this.generalCategory = SoundCategory.valueOf(name());
        }
    }

    private static final String INIT_FILE_NAME = "peak-init.syx";
    private static InitSysexMessage INIT_SYSEX;

    static {
        INIT_SYSEX = Utils.loadInitSysEx(INIT_FILE_NAME, MESSAGE_LENGTH);
    }

    public PeakSound() {
        this(-1, INIT_SYSEX, "");
    }

    public PeakSound(int id, SysexMessage sysEx, String soundSetName) {
        super(id, sysEx, soundSetName, PeakFactory.INSTANCE);
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
        byte[] msg = getSysEx().getMessage();
        int catId = msg[CATEGORY_OFFSET];
        return PeakCategory.values()[catId].generalCategory;
    }

    @Override
    protected void setCategoryImp(SoundCategory category) {
        PeakCategory peakCategory = PeakCategory.valueOf(category.name());
        updateSysEx(CATEGORY_OFFSET, (byte) peakCategory.ordinal());
    }

    @Override
    protected int getBankImp() {
        byte[] msg = getSysEx().getMessage();
        return msg[BANK_OFFSET] - 1; // Peak banks are 1-based
    }

    @Override
    protected int getProgramImp() {
        byte[] msg = getSysEx().getMessage();
        return msg[PROGRAM_OFFSET]; // Peak programs are 0-based
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
        data[REGULAR_PATCH_FLAG_OFFSET] = 0;
        data[BANK_OFFSET] = 0;
        data[PROGRAM_OFFSET] = 0;
        return data;
    }
}
