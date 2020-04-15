package org.iquality.cherubin;

import java.util.List;

public interface MultiSound extends Sound {

    interface SlotRef {
        int getBank();

        void setRef(int bank, int program);

        int getProgram();
    }

    List<SlotRef> getSlotRefs();

    @Override
    MultiSound clone(int programBank, int programNumber);
}
