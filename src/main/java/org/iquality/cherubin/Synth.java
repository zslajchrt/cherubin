package org.iquality.cherubin;

import org.iquality.cherubin.blofeld.BlofeldSingleSound;

import java.util.List;

public class Synth {

    private final int id;
    private final String name;
    private final List<List<Sound>> banks;
    private final List<MultiSound> multi;

    private boolean dirty;

    public Synth(int id, String name, List<List<Sound>> banks, List<MultiSound> multi) {
        this.id = id;
        this.name = name;
        this.banks = banks;
        this.multi = multi;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<List<Sound>> getBanks() {
        return banks;
    }

    public List<MultiSound> getMulti() {
        return multi;
    }

    public void updateSound(int bankNum, int slot, Sound sound) {
        banks.get(bankNum).set(slot, sound.clone((byte) bankNum, (byte) slot));
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void deleteSound(int bankNum, int slot) {
        banks.get(bankNum).set(slot, new BlofeldSingleSound());
        dirty = true;
    }

    void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

}
