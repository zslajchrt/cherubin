package org.iquality.cherubin;

import org.japura.gui.CheckComboBox;
import org.japura.gui.event.ListCheckListener;
import org.japura.gui.event.ListEvent;
import org.japura.gui.model.ListCheckModel;

import javax.sound.midi.MidiDevice;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MidiDevicesDialog extends CommonDialog {

    public MidiDevicesDialog(JFrame parent, MidiDeviceManager midiDeviceManager) {
        super(parent, "MIDI Devices Configuration", true);
        if (parent != null) {
            Dimension parentSize = parent.getSize();
            Point p = parent.getLocation();
            setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        }
        JPanel mainPane = new JPanel();
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.Y_AXIS));

        JLabel midiInLbl = adjust(new JLabel("MIDI In:"));
        mainPane.add(midiInLbl);
        CheckComboBox inCombo = makeMidiDeviceCombo(MidiDeviceManager.SystemDeviceType.input, midiDeviceManager);
        midiInLbl.setLabelFor(inCombo);
        mainPane.add(adjust(inCombo));

        JLabel midiOutLbl = adjust(new JLabel("MIDI Out:"));
        mainPane.add(midiOutLbl);
        CheckComboBox outCombo = makeMidiDeviceCombo(MidiDeviceManager.SystemDeviceType.output, midiDeviceManager);
        midiInLbl.setLabelFor(outCombo);
        mainPane.add(adjust(outCombo));

        JLabel midiCtrlLbl = adjust(new JLabel("MIDI Controller:"));
        mainPane.add(midiCtrlLbl);
        CheckComboBox ctrlCombo = makeMidiDeviceCombo(MidiDeviceManager.SystemDeviceType.controller, midiDeviceManager);
        midiInLbl.setLabelFor(ctrlCombo);
        mainPane.add(adjust(ctrlCombo));

        mainPane.setBorder(new EmptyBorder(5, 5, 5, 5));

        getContentPane().add(mainPane);

        JPanel buttonPane = new JPanel();

        JButton btnOK = new JButton(new AbstractAction("Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });
        buttonPane.add(btnOK);

        getRootPane().setDefaultButton(btnOK);

        getContentPane().add(buttonPane, BorderLayout.SOUTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        pack();
        setVisible(true);

    }

    private <T extends JComponent> T adjust(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static CheckComboBox makeMidiDeviceCombo(MidiDeviceManager.SystemDeviceType deviceType, MidiDeviceManager midiDeviceManager) {
        CheckComboBox ccb = new CheckComboBox();
        ccb.setTextFor(CheckComboBox.NONE, "* no " + deviceType + " selected *");
        ccb.setTextFor(CheckComboBox.ALL, "* all selected *");

        ListCheckModel ccbModel = ccb.getModel();
        MidiDeviceManager.getAvailableDevices(deviceType.isInput).forEach(ccbModel::addElement);
        java.util.List<MidiDevice> initialDevices = MidiDeviceManager.getInitialDevices(deviceType);

        ccb.setTextFor(CheckComboBox.MULTIPLE, MidiDeviceManager.getConcatDeviceNames(initialDevices));

        java.util.List<MidiDevice> selectedInitialDevices = new ArrayList<>();
        for (int i = 0; i < ccb.getModel().getSize(); i++) {
            MidiDevice devElem = (MidiDevice) ccbModel.getElementAt(i);
            if (initialDevices.contains(devElem)) {
                selectedInitialDevices.add(devElem);
            }
        }
        ccbModel.setCheck(selectedInitialDevices.toArray());

        ccb.getModel().addListCheckListener(new ListCheckListener() {
            @Override
            public void removeCheck(ListEvent listEvent) {
                changeSystemDeviceProvider(listEvent);
            }

            @Override
            public void addCheck(ListEvent listEvent) {
                changeSystemDeviceProvider(listEvent);
            }

            private void changeSystemDeviceProvider(ListEvent listEvent) {
                List<MidiDevice> selectedDevices = listEvent.getSource().getCheckeds().stream().
                        map(elem -> ((MidiDevice) elem)).
                        collect(Collectors.toList());
                midiDeviceManager.setSystemDeviceProvider(selectedDevices, deviceType);

                ccb.setTextFor(CheckComboBox.MULTIPLE, MidiDeviceManager.getConcatDeviceNames(selectedDevices));
            }

        });

        return ccb;
    }

}
