package org.iquality.cherubin;

import javax.sound.midi.*;
import java.io.IOException;

public class MidiProxy {

    public interface MidiProxyListener {
        MidiMessage onMessage(MidiMessage message, long timeStamp) throws Exception;
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
            MidiMessage alteredMessage = null;
            try {
                alteredMessage = listener.onMessage(message, timeStamp);
            } catch (Exception e) {
                e.printStackTrace();
                alteredMessage = message;
            }
            receiver.send(alteredMessage == null ? message : alteredMessage, timeStamp);
        }

        @Override
        public void close() {
            receiver.close();
        }
    }

    public static void main(String[] args) throws IOException {
        MidiDevice proxyIn = MidiDeviceManager.findDevice("Arturia", true);
        MidiDevice proxyOut = MidiDeviceManager.findDevice("VirtualMIDICable2", false);
        new MidiProxy(proxyIn, proxyOut, new MidiProxyListener() {
            @Override
            public MidiMessage onMessage(MidiMessage message, long timeStamp) throws InvalidMidiDataException {
                if (message instanceof ShortMessage) {
                    ShortMessage shortMessage = (ShortMessage) message;
                    int command = shortMessage.getCommand();
                    if (command == ShortMessage.NOTE_ON || command == ShortMessage.NOTE_OFF) {
                        int data1 = shortMessage.getData1();
                        int data2 = shortMessage.getData2();
                        int channel = shortMessage.getChannel();
                        switch (data1) {
                            case 0x25:
                                channel = 10;
                            default:
                        }

                        message = new ShortMessage(command, channel, data1, data2);
                        System.out.printf("%02X %02X %02X %02X\n", command, data1, data2, channel);
                    }

                }
                return message;
            }
        });

        System.in.read();

    }
}
