package org.iquality.cherubin;

import org.iquality.cherubin.blofeld.BlofeldSingleSound;

import java.util.List;

public class Synth extends SynthHeader {

    private final List<List<Sound>> banks;
    private final List<MultiSound> multi;

    private boolean dirty;

    public Synth(int id, String name, List<List<Sound>> banks, List<MultiSound> multi, SynthFactory synthFactory) {
        super(id, name, synthFactory);
        this.banks = banks;
        this.multi = multi;
    }

    public List<List<Sound>> getBanks() {
        return banks;
    }

    public List<MultiSound> getMulti() {
        return multi;
    }

    public boolean updateSound(int bankNum, int slot, Sound sound) {
        if (sound.getSynthFactory() != getSynthFactory()) {
            return false;
        }

        banks.get(bankNum).set(slot, sound.clone((byte) bankNum, (byte) slot));
        dirty = true;

        return true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void deleteSound(int bankNum, int slot) {
        banks.get(bankNum).set(slot, getSynthFactory().createSingleSound());
        setDirty(true);
    }

    public void deleteMultiSound(int slot) {
        multi.set(slot, getSynthFactory().createMultiSound());
        setDirty(true);
    }

    void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

}
