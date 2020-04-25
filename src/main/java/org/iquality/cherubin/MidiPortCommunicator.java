package org.iquality.cherubin;

import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiException;

import javax.sound.midi.MidiDevice;

@Deprecated
public class MidiPortCommunicator {

    protected final MidiDevice device;

    public MidiPortCommunicator(MidiDevice device) {
        this.device = device;
    }

    public MidiPortCommunicator(String deviceName, boolean isInput) {
        try {
            if (!isCoreMidiLoaded()) {
                throw new RuntimeException("CoreMIDI4J native library is not available.");
            }

            device = MidiDeviceManager.findDevice(deviceName, isInput);
        } catch (CoreMidiException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isCoreMidiLoaded() throws CoreMidiException {
        return CoreMidiDeviceProvider.isLibraryLoaded();
    }

    public void close() {
        device.close();
    }
}
