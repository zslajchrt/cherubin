package org.iquality.cherubin;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class MidiProxyInterceptor implements MidiDevice, MidiProxy.MidiProxyListener {

    private final Function<MidiProxy.Direction, Boolean> directionDecider;

    private final List<Receiver> receivers = Collections.synchronizedList(new ArrayList<>());
    private final List<MidiProxyTransmitter> transmitters = Collections.synchronizedList(new ArrayList<>());

    private boolean open;

    public MidiProxyInterceptor(Function<MidiProxy.Direction, Boolean> directionDecider) {
        this.directionDecider = directionDecider;
    }

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
        return 0;
    }

    @Override
    public int getMaxTransmitters() {
        return -1;
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return null;
    }

    @Override
    public List<Receiver> getReceivers() {
        return null;
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        return new MidiProxyTransmitter();
    }

    @Override
    public List<Transmitter> getTransmitters() {
        return Collections.unmodifiableList(transmitters);
    }

    @Override
    public MidiMessage onMessage(MidiMessage message, long timeStamp, MidiProxy.Direction direction) {
        if (!this.directionDecider.apply(direction)) {
            return message;
        }

        synchronized (transmitters) {
            for (MidiProxyTransmitter transmitter : transmitters) {
                try {
                    transmitter.getReceiver().send(message, timeStamp);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        return message;
    }

    class MidiProxyTransmitter implements MidiDeviceTransmitter {

        private Receiver receiver;

        {
            transmitters.add(this);
        }

        @Override
        public MidiDevice getMidiDevice() {
            return MidiProxyInterceptor.this;
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
    }
}
