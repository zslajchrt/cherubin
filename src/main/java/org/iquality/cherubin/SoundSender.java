package org.iquality.cherubin;

import javax.sound.midi.*;

public class SoundSender {

    public static final int SOUND_DUMP_DELAY = 500;

    private final ShortMessage probeNoteOn;
    private final ShortMessage probeNoteOff;

    private final AppModel appModel;

    public SoundSender(AppModel appModel) {
        this.appModel = appModel;
        try {
            probeNoteOn = new ShortMessage();
            // Start playing the note Middle C (60),
            // moderately loud (velocity = 93).
            probeNoteOn.setMessage(ShortMessage.NOTE_ON, 0, 60, 93);

            probeNoteOff = new ShortMessage();
            // Start playing the note Middle C (60),
            // moderately loud (velocity = 93).
            probeNoteOff.setMessage(ShortMessage.NOTE_OFF, 0, 60, 0);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Receiver getReceiver(AppModel.OutputDirection outputDirection) {
        try {
            return appModel.getOutputDevice(outputDirection).getReceiver();
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void probeNoteOn() {
        getReceiver(getDefaultDirection()).send(probeNoteOn, -1);
    }

    public void probeNoteOff() {
        getReceiver(getDefaultDirection()).send(probeNoteOff, -1);
    }

    public void sendSoundWithDelay(SingleSound sound) {
        sendSoundWithDelay(sound, getDefaultDirection());
    }

    public void sendSoundWithDelay(SingleSound sound, AppModel.OutputDirection outputDirection) {
        sendSound(sound, outputDirection);
        try {
            Thread.sleep(SOUND_DUMP_DELAY);
        } catch (InterruptedException ignored) {
        }
    }

    public void sendSound(SingleSound sound, boolean sendToEditBuffer) {
        sendSound(sound, sendToEditBuffer, getDefaultDirection());
    }

    private AppModel.OutputDirection getDefaultDirection() {
        return appModel.getDefaultOutputDirection();
    }

    public void sendSound(SingleSound sound, AppModel.OutputDirection outputDirection) {
        sendSound(sound, false, outputDirection);
    }

    public void sendSound(SingleSound sound, boolean sendToEditBuffer, AppModel.OutputDirection outputDirection) {
        if (sendToEditBuffer) {
            getReceiver(outputDirection).send(sound.cloneForEditBuffer().sysEx, -1);
        } else {
            System.out.println("Sending " + sound.name + " to bank " + sound.getBank() + " and slot " + sound.getSlot());
            getReceiver(outputDirection).send(sound.sysEx, -1);
        }
    }
}
