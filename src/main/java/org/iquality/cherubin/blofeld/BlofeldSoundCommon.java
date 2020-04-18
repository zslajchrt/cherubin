package org.iquality.cherubin.blofeld;

import org.iquality.cherubin.Sound;

public interface BlofeldSoundCommon extends Sound {

    int MESSAGE_ID_OFFSET = 4;
    int BANK_OFFSET = 5;
    int PROGRAM_OFFSET = 6;
    int SDATA_OFFSET = 7;
    int SDATA_LENGTH = 380;
    int SINGLE_NAME_OFFSET = 363;
    int NAME_LENGTH = 16;
    int SINGLE_CAT_OFFSET = 379;
    int MULTI_NAME_OFFSET = SDATA_OFFSET;
    int MULTI_SLOTS_OFFSET = MULTI_NAME_OFFSET + 32;
    int MULTI_SLOT_LENGTH = 24;

    byte SINGLE_DUMP = 0x10;
    byte MULTI_DUMP = 0x11;

}
