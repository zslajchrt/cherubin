package org.iquality.cherubin;

public interface MultiSound extends Sound {

    class SlotRef {
        private final int bank;
        private final int program;

        public SlotRef(int bank, int program) {
            this.bank = bank;
            this.program = program;
        }

        @Override
        public String toString() {
            return String.format("%s%s", (char) ('A' + bank), program + 1);
        }
    }

    SlotRef[] getSlotRefs();
}
