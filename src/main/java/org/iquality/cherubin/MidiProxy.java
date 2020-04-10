package org.iquality.cherubin;

import javax.sound.midi.*;
import java.io.IOException;

import static org.iquality.cherubin.MidiPortCommunicator.findDevice;

public class MidiProxy {

    public enum Direction {
        leftToRight,
        rightToLeft,
    }

    public interface MidiProxyListener {
        MidiMessage onMessage(MidiMessage message, long timeStamp, Direction direction);
    }

    private final MidiDevice leftIn;
    private final Transmitter leftInTransmitter;
    private final MidiDevice leftOut;
    private final Receiver leftOutReceiver;
    private final MidiDevice rightIn;
    private final Transmitter rightInTransmitter;
    private final MidiDevice rightOut;
    private final Receiver rightOutReceiver;

    public MidiProxy(AppModel appModel) {
        this(appModel, (msg, ts, dir) -> msg);
    }

    public MidiProxy(AppModel appModel, MidiProxyListener listener) {
        this.leftIn = appModel.getInputDevice(AppModel.InputDirection.left);
        this.leftOut = appModel.getOutputDevice(AppModel.OutputDirection.left);
        this.rightIn = appModel.getInputDevice(AppModel.InputDirection.right);
        this.rightOut = appModel.getOutputDevice(AppModel.OutputDirection.right);

        try {
            leftInTransmitter = leftIn.getTransmitter();
            rightInTransmitter = rightIn.getTransmitter();

            leftOutReceiver = leftOut.getReceiver();
            rightOutReceiver = rightOut.getReceiver();

            leftInTransmitter.setReceiver(new CapturingReceiver(rightOutReceiver, Direction.leftToRight, listener));
            rightInTransmitter.setReceiver(new CapturingReceiver(leftOutReceiver, Direction.rightToLeft, listener));
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            try {
                this.leftIn.close();
            } finally {
                try {
                    this.leftOut.open();
                } finally {
                    try {
                        this.rightIn.open();
                    } finally {
                        this.rightOut.open();
                    }
                }
            }
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }


    static class CapturingReceiver implements Receiver {

        private final Receiver receiver;
        private final Direction direction;
        private final MidiProxyListener listener;

        public CapturingReceiver(Receiver receiver, Direction direction, MidiProxyListener listener) {
            this.receiver = receiver;
            this.direction = direction;
            this.listener = listener;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            MidiMessage alteredMessage = listener.onMessage(message, timeStamp, direction);
            receiver.send(alteredMessage == null ? message : alteredMessage, timeStamp);
        }

        @Override
        public void close() {
            receiver.close();
        }
    }
}
