package org.iquality.cherubin;

import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class MidiActivityPanel extends JPanel {

    private final MidiProxy proxy;
    private final MidiDeviceManager midiDeviceManager;
    private final JPanel inSlots;
    private final JPanel outSlots;

    public MidiActivityPanel(MidiDeviceManager midiDeviceManager, MidiProxy proxy) {
        this.midiDeviceManager = midiDeviceManager;
        this.proxy = proxy;

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        inSlots = new JPanel();
        inSlots.setLayout(new BoxLayout(inSlots, BoxLayout.LINE_AXIS));
        add(inSlots);

        outSlots = new JPanel();
        outSlots.setLayout(new BoxLayout(outSlots, BoxLayout.LINE_AXIS));
        add(outSlots);

        HashSet<MidiDevice> involvedMidiIn = new HashSet<>(MidiDeviceManager.getAvailableDevices(true));
        HashSet<MidiDevice> involvedMidiOut = new HashSet<>(MidiDeviceManager.getAvailableDevices(false));

        int width = 25 * Math.max(involvedMidiIn.size(), involvedMidiOut.size());
        setPreferredSize(new Dimension(width, 50));
        setMaximumSize(new Dimension(width,50));

        buildContent(involvedMidiIn, true);
        buildContent(involvedMidiOut, false);

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                }
                checkActivity(inSlots);
                checkActivity(outSlots);
            }
        }).start();
    }

    private void checkActivity(JPanel slotsPanel) {
        Component[] children;
        children = slotsPanel.getComponents();
        for (Component child : children) {
            if (child instanceof MidiDeviceSlot) {
                ((MidiDeviceSlot) child).checkActivity();
            }
        }
    }

    private void buildContent(Collection<MidiDevice> devices, boolean midiIn) {
        JPanel slotsPanel = midiIn ? inSlots : outSlots;

        if (devices.isEmpty()) {
            JPanel emptySlot = new JPanel();
            emptySlot.setBackground(Color.LIGHT_GRAY);
            slotsPanel.add(emptySlot);
            return;
        }

        for (MidiDevice device : devices) {
            assert device instanceof MidiDeviceManager.MidiDeviceWrapper;
            MidiDeviceSlot slot = new MidiDeviceSlot(midiIn, (MidiDeviceManager.MidiDeviceWrapper) device);
            slotsPanel.add(slot);
        }

    }

    private void removeContent(boolean midiIn) {
        JPanel slotsPanel = midiIn ? inSlots : outSlots;
        for (Component child : slotsPanel.getComponents()) {
            if (child instanceof MidiDeviceSlot) {
                ((MidiDeviceSlot) child).close();
            }
        }

        slotsPanel.removeAll();
    }

    static class MidiDeviceSlot extends JPanel implements MidiDeviceManager.MessageListener {
        private volatile boolean timingActivity;
        private volatile boolean activeSensingActivity;
        private volatile boolean otherActivity;
        private final boolean midiIn;
        private MidiDeviceManager.MidiDeviceWrapper device;
        private Transmitter monitorTransmitter;

        //private final JPopupMenu popup;

        MidiDeviceSlot(boolean midiIn, MidiDeviceManager.MidiDeviceWrapper device) {
            this.device = device;
            this.midiIn = midiIn;
            setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            setBackground(Color.WHITE);
            setToolTipText((midiIn ? "In: " : "Out: ") + MidiDeviceManager.getDeviceName(device));

            device.addActivityListener(this, midiIn);

            if (midiIn) {
            //if (false) {
                try {
                    device.open();
                    monitorTransmitter = device.getTransmitter();
                } catch (MidiUnavailableException e) {
                    throw new RuntimeException(e);
                }
                monitorTransmitter.setReceiver(new Receiver() {
                    @Override
                    public void send(MidiMessage message, long timeStamp) {

                    }

                    @Override
                    public void close() {

                    }
                });
            } else {
                monitorTransmitter = null;
            }

            //popup = new JPopupMenu();
            //popup.add(new JCheckBoxMenuItem("Always Monitor"));
            //addMouseListener(new MousePopupListener());
        }

//        class MousePopupListener extends MouseAdapter {
//            public void mousePressed(MouseEvent e) {
//                checkPopup(e);
//            }
//
//            public void mouseClicked(MouseEvent e) {
//                checkPopup(e);
//            }
//
//            public void mouseReleased(MouseEvent e) {
//                checkPopup(e);
//            }
//
//            private void checkPopup(MouseEvent e) {
//                if (e.isPopupTrigger()) {
//                    popup.show(MidiDeviceSlot.this, e.getX(), e.getY());
//                }
//            }
//        }

        public synchronized void close() {
            device.removeActivityListener(this, midiIn);
            if (monitorTransmitter != null) {
                monitorTransmitter.close();
                monitorTransmitter = null;
                device.close();
            }
        }

        /**
         * Called from a background thread in short regular intervals.
         */
        void checkActivity() {
            int g = 0x40;
            int r = 0x40;
            int b = 0x40;

            if (timingActivity) {
                g += 0x40;
            }

            if (activeSensingActivity) {
                g += 0x40;
            }

            if (otherActivity) {
                g = 0xFF;
            }

            setBackground(new Color(r, g, b));
            timingActivity = false;
            activeSensingActivity = false;
            otherActivity = false;
            repaint();
        }

        @Override
        public MidiMessage onMessage(MidiMessage message, long timeStamp) throws Exception {
            switch (message.getStatus()) {
                case ShortMessage.TIMING_CLOCK:
                    timingActivity = true;
                    break;
                case ShortMessage.ACTIVE_SENSING:
                    activeSensingActivity = true;
                    break;
                default:
                    otherActivity = true;
            }

            return message;
        }
    }
}
