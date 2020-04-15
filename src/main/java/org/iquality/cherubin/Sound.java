package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;

public interface Sound {

    SynthFactory getSynthFactory();

    int getId();

    void setSysEx(SysexMessage sysEx);

    String getName();

    SoundCategory getCategory();

    SysexMessage getSysEx();

    String getSoundSetName();

    boolean isEmpty();

    default boolean nonEmpty() {
        return !isEmpty();
    }

    int getBank();

    int getProgram();

    Sound clone(int programBank, int programNumber);

    Sound cloneForEditBuffer();

    void setCategory(SoundCategory value);
}
