package org.iquality.cherubin.bassStation2;

import org.iquality.cherubin.*;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import java.util.ArrayList;
import java.util.List;

public class BS2Factory implements SynthFactory {

    public static final int BANK_COUNT = 1;
    public static final int BANK_SIZE = 128;

    public static final SynthFactory INSTANCE = new BS2Factory();
    public static final String SYNTH_ID = "BassStation 2";

    @Override
    public String getSynthId() {
        return SYNTH_ID;
    }

    @Override
    public List<Sound> createSounds(SysexMessage sysexMessage, String soundSetName) {
        byte[] bulkMsg = sysexMessage.getMessage();
        int msgCount = bulkMsg.length / BS2Sound.MESSAGE_LENGTH;

        if (bulkMsg.length % BS2Sound.MESSAGE_LENGTH != 0) {
            throw new RuntimeException("Invalid bulk sysex for Bass Station 2");
        }

        List<Sound> sounds = new ArrayList<>();

        for (int i = 0; i < msgCount; i++) {
            byte[] singleMsg = new byte[BS2Sound.MESSAGE_LENGTH];
            System.arraycopy(bulkMsg, i * BS2Sound.MESSAGE_LENGTH, singleMsg, 0, BS2Sound.MESSAGE_LENGTH);
            BS2Sound sound;
            try {
                sound = new BS2Sound(-1, new SysexMessage(singleMsg, singleMsg.length), SoundCategory.Init, soundSetName);
            } catch (InvalidMidiDataException e) {
                throw new RuntimeException(e);
            }
            sounds.add(sound);
        }

        return sounds;
    }

    @Override
    public BS2Sound createOneSound(int id, String name, SysexMessage sysexMessage, SoundCategory category, String soundSetName) {
        return new BS2Sound(id, sysexMessage, category, soundSetName);
    }

    @Override
    public SingleSound createSingleSound() {
        return new BS2Sound();
    }

    @Override
    public MultiSound createMultiSound() {
        throw new UnsupportedOperationException("No multi sound in BassStation 2");
    }

    @Override
    public int getBankCount() {
        return BANK_COUNT;
    }

    @Override
    public int getBankSize() {
        return BANK_SIZE;
    }

    @Override
    public boolean hasMultiBank() {
        return false;
    }

    @Override
    public int getMultiBankSize() {
        return 0;
    }

    @Override
    public int getMultiSlotCount() {
        return 0;
    }

    @Override
    public boolean isMulti(SysexMessage sysex) {
        return false;
    }

    @Override
    public boolean accepts(SysexMessage message) {
        byte[] msg = message.getMessage();
        return msg.length % BS2Sound.MESSAGE_LENGTH == 0 && msg[0] == (byte) 0xF0 && msg[1] == (byte) 0x00 && msg[2] == (byte) 0x20 && msg[3] == (byte) 0x29;
    }

    @Override
    public String toString() {
        return "Bass Station 2";
    }
}
