package org.iquality.cherubin;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NullMidiPort implements MidiDevice {

    final List<Receiver> receivers = Collections.synchronizedList(new ArrayList<>());
    final List<Transmitter> transmitters = Collections.synchronizedList(new ArrayList<>());

    private boolean open;

    @Override
    public Info getDeviceInfo() {
        return null;
    }

    @Override
    public void open() throws MidiUnavailableException {
        open = true;
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public long getMicrosecondPosition() {
        return -1;
    }

    @Override
    public int getMaxReceivers() {
        return -1;
    }

    @Override
    public int getMaxTransmitters() {
        return -1;
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return new MidiDeviceReceiver() {

            {
                receivers.add(this);
            }

            @Override
            public MidiDevice getMidiDevice() {
                return NullMidiPort.this;
            }

            @Override
            public void send(MidiMessage message, long timeStamp) {

            }

            @Override
            public void close() {
                receivers.remove(this);
            }
        };
    }

    @Override
    public List<Receiver> getReceivers() {
        return Collections.unmodifiableList(receivers);
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        return new MidiDeviceTransmitter() {

            private Receiver receiver;

            {
                transmitters.add(this);
            }

            @Override
            public MidiDevice getMidiDevice() {
                return NullMidiPort.this;
            }

            @Override
            public void setReceiver(Receiver receiver) {
                this.receiver = receiver;
            }

            @Override
            public Receiver getReceiver() {
                return receiver;
            }

            @Override
            public void close() {
                transmitters.remove(this);
            }
        };
    }

    @Override
    public List<Transmitter> getTransmitters() {
        return Collections.unmodifiableList(transmitters);
    }
}
