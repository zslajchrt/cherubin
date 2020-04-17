package org.iquality.cherubin;

import javax.sound.midi.*;
import java.io.IOException;

import static org.iquality.cherubin.MidiPortCommunicator.findDevice;

public class MidiProxy {

    public interface MidiProxyListener {
        MidiMessage onMessage(MidiMessage message, long timeStamp);
    }

    private final MidiDevice in;
    private final MidiDevice out;
    private final Transmitter transmitter;
    private final Receiver receiver;

    public MidiProxy(MidiDevice in, MidiDevice out, MidiProxyListener listener) {
        try {
            this.in = open(in);
            this.out = open(out);

            this.transmitter = in.getTransmitter();
            this.receiver = out.getReceiver();

            transmitter.setReceiver(new CapturingReceiver(receiver, listener));
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    private MidiDevice open(MidiDevice device) throws MidiUnavailableException {
        device.open();
        return device;
    }

    public void close() {
        try {
            this.receiver.close();
        } finally {
            this.transmitter.close();
        }
    }

    static class CapturingReceiver implements Receiver {

        private final Receiver receiver;
        private final MidiProxyListener listener;

        public CapturingReceiver(Receiver receiver, MidiProxyListener listener) {
            this.receiver = receiver;
            this.listener = listener;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            MidiMessage alteredMessage = listener.onMessage(message, timeStamp);
            receiver.send(alteredMessage == null ? message : alteredMessage, timeStamp);
        }

        @Override
        public void close() {
            receiver.close();
        }
    }
}
