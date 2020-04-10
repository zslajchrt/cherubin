package org.iquality.cherubin;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DuplexMidiPort implements MidiDevice {

    final List<Receiver> receivers = Collections.synchronizedList(new ArrayList<>());
    final List<Transmitter> transmitters = Collections.synchronizedList(new ArrayList<>());

    private final MidiDevice device1;
    private final MidiDevice device2;

    public DuplexMidiPort(MidiDevice device1, MidiDevice device2) {
        this.device1 = device1;
        this.device2 = device2;
    }

    @Override
    public Info getDeviceInfo() {
        return null;
    }

    @Override
    public void open() throws MidiUnavailableException {
        if (!device1.isOpen()) {
            device1.open();
        }
        if (device2.isOpen()) {
            device2.open();
        }
    }

    @Override
    public void close() {
        try {
            device1.close();
        } finally {
            device2.close();
        }
    }

    @Override
    public boolean isOpen() {
        return device1.isOpen() && device2.isOpen();
    }

    @Override
    public long getMicrosecondPosition() {
        return -1;
    }

    @Override
    public int getMaxReceivers() {
        int max1 = device1.getMaxReceivers();
        int max2 = device2.getMaxReceivers();
        max1 = max1 < 0 ? Integer.MAX_VALUE : max1;
        max2 = max2 < 0 ? Integer.MAX_VALUE : max2;
        return Math.min(max1, max2);
    }

    @Override
    public int getMaxTransmitters() {
        int max1 = device1.getMaxTransmitters();
        int max2 = device2.getMaxTransmitters();
        max1 = max1 < 0 ? Integer.MAX_VALUE : max1;
        max2 = max2 < 0 ? Integer.MAX_VALUE : max2;
        return Math.min(max1, max2);
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return new MidiDeviceReceiver() {

            private final Receiver receiver1 = device1.getReceiver();
            private final Receiver receiver2 = device2.getReceiver();

            {
                receivers.add(this);
            }

            @Override
            public MidiDevice getMidiDevice() {
                return DuplexMidiPort.this;
            }

            @Override
            public void send(MidiMessage message, long timeStamp) {
                receiver1.send(message, timeStamp);
                receiver2.send(message, timeStamp);
            }

            @Override
            public void close() {
                receivers.remove(this);
                try {
                    receiver1.close();
                } finally {
                    receiver2.close();
                }
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

            private final Transmitter transmitter1 = device1.getTransmitter();
            private final Transmitter transmitter2 = device2.getTransmitter();

            {
                transmitters.add(this);
            }

            @Override
            public MidiDevice getMidiDevice() {
                return DuplexMidiPort.this;
            }

            @Override
            public void setReceiver(Receiver receiver) {
                transmitter1.setReceiver(receiver);
                transmitter2.setReceiver(receiver);
            }

            @Override
            public Receiver getReceiver() {
                return transmitter1.getReceiver();
            }

            @Override
            public void close() {
                transmitters.remove(this);
                try {
                    transmitter1.close();
                } finally {
                    transmitter2.close();
                }
            }
        };
    }

    @Override
    public List<Transmitter> getTransmitters() {
        return Collections.unmodifiableList(transmitters);
    }
}
