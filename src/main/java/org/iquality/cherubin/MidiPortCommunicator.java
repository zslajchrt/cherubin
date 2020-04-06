package org.iquality.cherubin;

import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiException;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

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

            device = findDevice(deviceName, isInput);
            if (device == null) {
                throw new RuntimeException((isInput ? "Input " : "Output") + " device " + deviceName + " not found");
            }
        } catch (CoreMidiException | MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isCoreMidiLoaded() throws CoreMidiException {
        return CoreMidiDeviceProvider.isLibraryLoaded();
    }

    static MidiDevice findDevice(String deviceName, boolean midiIn) throws MidiUnavailableException {
        for (javax.sound.midi.MidiDevice.Info deviceInfo : CoreMidiDeviceProvider.getMidiDeviceInfo()) {
            if (deviceInfo.getName().equals(deviceName)) {
                MidiDevice device = MidiSystem.getMidiDevice(deviceInfo);
                int max = midiIn ? device.getMaxTransmitters() : device.getMaxReceivers();
                if (max != 0) {
                    return device;
                }
            }
        }
        return null;
    }

    public void close() {
        device.close();
    }
}
