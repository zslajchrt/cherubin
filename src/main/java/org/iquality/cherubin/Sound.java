package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;

public interface Sound {

    void initialize();

    SynthFactory getSynthFactory();

    int getId();

    void setSysEx(SysexMessage sysEx);

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

    int getProgram();

    Sound clone(int programBank, int programNumber);

    Sound cloneForEditBuffer();

    void verify() throws VerificationException;

    class VerificationException extends Exception {
        public VerificationException(String message) {
            super(message);
        }
    }
}
