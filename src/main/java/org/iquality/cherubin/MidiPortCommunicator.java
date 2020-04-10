package org.iquality.cherubin;

import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiException;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

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

            device = findDevice(deviceName, isInput);
        } catch (CoreMidiException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isCoreMidiLoaded() throws CoreMidiException {
        return CoreMidiDeviceProvider.isLibraryLoaded();
    }

    public static MidiDevice findDevice(String deviceName, boolean midiIn) {
        try {
            for (MidiDevice.Info deviceInfo : CoreMidiDeviceProvider.getMidiDeviceInfo()) {
                if (deviceInfo.getName().contains(deviceName)) {
                    MidiDevice device = MidiSystem.getMidiDevice(deviceInfo);
                    int max = midiIn ? device.getMaxTransmitters() : device.getMaxReceivers();
                    if (max != 0) {
                        return device;
                    }
                }
            }
            throw new RuntimeException((midiIn ? "Input " : "Output") + " device " + deviceName + " not found");
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        device.close();
    }
}
