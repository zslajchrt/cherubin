package org.iquality.cherubin;

import javax.sound.midi.SysexMessage;

public interface Sound {

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

}
