package org.iquality.cherubin;

import java.util.List;

public class VirtualBlofeld {
    public final int id;
    public final String name;
    public final List<List<SingleSound>> banks;
    public final List<MultiSound> multiBank;
    private boolean dirty;

    public VirtualBlofeld(int id, String name, List<List<SingleSound>> banks, List<MultiSound> multiBank) {
        this.id = id;
        this.name = name;
        this.banks = banks;
        this.multiBank = multiBank;
    }

    public void updateSound(int bankNum, int slot, SingleSound sound) {
        banks.get(bankNum).set(slot, (SingleSound) sound.clone((byte) bankNum, (byte) slot));
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isInitial() {
        return VirtualBlofeldModel.INIT_BLOFELD.equals(name);
    }

    public void deleteSound(int bankNum, int slot) {
        banks.get(bankNum).set(slot, VirtualBlofeldModel.EMPTY_SOUND);
        dirty = true;
    }

    void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
