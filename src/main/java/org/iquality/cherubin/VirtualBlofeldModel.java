package org.iquality.cherubin;

import java.util.ArrayList;
import java.util.List;

public class VirtualBlofeldModel {

    public static final int BANKS_NUMBER = 8;
    public static final int BANK_SIZE = 128;

    private final List<List<SingleSound>> banks;
    private final List<MultiSound> multiBank;
    private final SoundDbModel soundDbModel;

    public VirtualBlofeldModel(SoundDbModel soundDbModel) {
        this.soundDbModel = soundDbModel;
        this.banks = new ArrayList<>();
        for (int bank = 0; bank < BANKS_NUMBER; bank++) {
            banks.add(initBank((byte) bank));
        }
        this.multiBank = initMultiBank();
    }

    private List<SingleSound> initBank(byte bank) {
        List<SingleSound> bankList = new ArrayList<>();
        SingleSound initSound = soundDbModel.getInitSound();
        for (int program = 0; program < BANK_SIZE; program++) {
            bankList.add((SingleSound) initSound.clone(bank, (byte) program));
        }
        return bankList;
    }

    private List<MultiSound> initMultiBank() {
        List<MultiSound> bankList = new ArrayList<>();
        MultiSound initSound = soundDbModel.getInitMulti();
        for (int program = 0; program < BANK_SIZE; program++) {
            bankList.add((MultiSound) initSound.clone((byte) 0, (byte) program));
        }
        return bankList;
    }

    public List<SingleSound> getBank(int i) {
        return banks.get(i);
    }

    public List<MultiSound> getMultiBank() {
        return multiBank;
    }
}
