package org.iquality.cherubin;

import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiNotification;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiException;
import javax.sound.midi.MidiDevice;

public class Example {

    public static boolean isCoreMidiLoaded() throws CoreMidiException {
        return CoreMidiDeviceProvider.isLibraryLoaded();
    }

    public static void watchForMidiChanges() throws CoreMidiException {
        CoreMidiDeviceProvider.addNotificationListener(new CoreMidiNotification() {
            public void midiSystemUpdated() {
                System.out.println("The MIDI environment has changed.");
            }
        });
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Working MIDI Devices:");
        for (javax.sound.midi.MidiDevice.Info device : CoreMidiDeviceProvider.getMidiDeviceInfo()) {
            System.out.println("  " + device.getName());
        }

        if (Example.isCoreMidiLoaded()) {
            System.out.println("CoreMIDI4J native library is running.");
        } else {
            System.out.println("CoreMIDI4J native library is not available.");
        }

        watchForMidiChanges();
        System.out.println("Watching for MIDI environment changes for thirty seconds.");
        Thread.sleep(30000);
    }
}