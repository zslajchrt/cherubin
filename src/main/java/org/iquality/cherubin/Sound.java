package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;

public interface Sound {

    void initialize();

    SynthFactory getSynthFactory();

    int getId();

    void setSysEx(SysexMessage sysEx);

    default void setSysEx(SysexMessage sysEx, boolean restoreBankAndProgram) {
        if (restoreBankAndProgram) {
            int savedBank = getBank();
            int savedProgram = getProgram();
            setSysEx(sysEx);
            setBank(savedBank);
            setProgram(savedProgram);
        } else {
            setSysEx(sysEx);
        }
    }

    String getName();

    void setName(String name);

    SoundCategory getCategory();

    void setCategory(SoundCategory value);

    SysexMessage getSysEx();

    String getSoundSetName();

    boolean isInit();

    default boolean isRegular() {
        return !isInit();
    }

    int getBank();

    void setBank(int bank);

    int getProgram();

    void setProgram(int program);

    Sound clone();

    default Sound clone(int programBank, int programNumber) {
        Sound cloned = clone();
        cloned.setBank(programBank);
        cloned.setProgram(programNumber);
        return cloned;
    }

    Sound cloneForEditBuffer();

    void verify() throws VerificationException;

    class VerificationException extends Exception {
        public VerificationException(String message) {
            super(message);
        }
    }
}
