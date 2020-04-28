package org.iquality.cherubin;

import com.sun.media.sound.MidiUtils;

import javax.sound.midi.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppModel {

    private final MidiDeviceManager midiDeviceManager;
    private final MidiProxy proxy;
    private final MidiServices midiServices;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public AppModel(MidiDeviceManager midiDeviceManager, MidiProxy proxy, MidiServices midiServices) {
        this.midiDeviceManager = midiDeviceManager;
        this.proxy = proxy;
        this.midiServices = midiServices;
    }

    public void close() {
    }

    public MidiServices getMidiServices() {
        return midiServices;
    }

    public MidiDeviceManager getMidiDeviceManager() {
        return midiDeviceManager;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public MidiDevice getInputDevice() {
        return midiDeviceManager.getInputDevice();
    }

    public MidiDevice getInputDevice(int inputVariant) {
        return midiDeviceManager.getInputDevice(inputVariant);
    }

    public MidiDevice getInputDevice(SynthFactory synthFactory, int inputVariant) {
        return midiDeviceManager.getInputDevice(synthFactory, inputVariant);
    }

    public MidiDevice getOutputDevice() {
        return midiDeviceManager.getOutputDevice();
    }

    public MidiDevice getOutputDevice(int outputVariant) {
        return midiDeviceManager.getOutputDevice(outputVariant);
    }

    public MidiDevice getOutputDevice(SynthFactory synthFactory) {
        return midiDeviceManager.getOutputDevice(synthFactory);
    }

    public MidiDevice getOutputDevice(SynthFactory synthFactory, int outputVariant) {
        return midiDeviceManager.getOutputDevice(synthFactory, outputVariant);
    }

    public MidiProxy getProxy() {
        return proxy;
    }

}
