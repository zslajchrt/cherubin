package org.iquality.cherubin;

public interface SoundSlotRef {
    int getBank();

    void setRef(int bank, int program);

    int getProgram();
}
