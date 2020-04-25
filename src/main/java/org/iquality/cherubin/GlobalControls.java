package org.iquality.cherubin;

import org.japura.gui.CheckComboBox;
import org.japura.gui.SplitButton;
import org.japura.gui.event.ListCheckListener;
import org.japura.gui.event.ListEvent;
import org.japura.gui.model.ListCheckModel;

import javax.sound.midi.MidiDevice;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GlobalControls {

    private static final String THRU_ON = "Thru On";
    private static final String THRU_OFF = "Thru Off";

    public static void makeToolBar(JToolBar toolBar, AppFrame appFrame) {
        toolBar.add(new JLabel("In:"));
        CheckComboBox ccb = makeMidiDeviceCombo(true, appFrame.getAppModel().getMidiDeviceManager());
        toolBar.add(ccb);

        toolBar.add(new JLabel("Out:"));
        ccb = makeMidiDeviceCombo(false, appFrame.getAppModel().getMidiDeviceManager());
        toolBar.add(ccb);

        toolBar.addSeparator(new Dimension(5,0));

        SplitButton midiThruBtn = new SplitButton(SplitButton.MENU);
        midiThruBtn.setText("MIDI Thru...");
        midiThruBtn.addButton(new AbstractAction(THRU_ON) {
            @Override
            public void actionPerformed(ActionEvent e) {
                appFrame.getAppModel().getProxy().setThru(true);
                midiThruBtn.setButtonEnabled(THRU_OFF, true);
                midiThruBtn.setButtonEnabled(THRU_ON, false);
            }
        });
        midiThruBtn.addButton(new AbstractAction(THRU_OFF) {
            @Override
            public void actionPerformed(ActionEvent e) {
                appFrame.getAppModel().getProxy().setThru(false);
                midiThruBtn.setButtonEnabled(THRU_OFF, false);
                midiThruBtn.setButtonEnabled(THRU_ON, true);
            }
        });
        if (appFrame.getAppModel().getProxy().isThru()) {
            midiThruBtn.setButtonEnabled(THRU_OFF, true);
            midiThruBtn.setButtonEnabled(THRU_ON, false);
        }

        midiThruBtn.addButton(new AbstractAction("Thru Config") {
            @Override
            public void actionPerformed(ActionEvent e) {
                MidiProxy proxy = appFrame.getAppModel().getProxy();
                new MidiThruDialog((JFrame) SwingUtilities.windowForComponent(toolBar), proxy.getLinks(), proxy::setLinks);
            }
        });

//        JButton midiThru = new JButton(new AbstractAction("Thru") {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                MidiProxy proxy = appFrame.getAppModel().getProxy();
//                new MidiThruDialog((JFrame) SwingUtilities.windowForComponent(toolBar), proxy.getLinks(), proxy::setLinks);
//            }
//        });
        toolBar.add(midiThruBtn);

        toolBar.addSeparator(new Dimension(5,0));

        toolBar.add(new JButton(new AbstractAction("All Notes Off") {
            @Override
            public void actionPerformed(ActionEvent e) {
                appFrame.getAppModel().getExecutor().execute(MidiServices::sendAllSoundsOff);
            }
        }));


        toolBar.add(Box.createHorizontalStrut(300));
    }

    private static CheckComboBox makeMidiDeviceCombo(boolean input, MidiDeviceManager midiDeviceManager) {
        CheckComboBox ccb = new CheckComboBox();
        String dir = input ? "input" : "output";
        ccb.setTextFor(CheckComboBox.NONE, "* no " + dir + " selected *");
        ccb.setTextFor(CheckComboBox.ALL, "* all selected *");

        ListCheckModel ccbModel = ccb.getModel();
        MidiDeviceManager.getAvailableDevices(input).forEach(ccbModel::addElement);
        List<MidiDevice> initialDevices = MidiDeviceManager.getInitialDevices(input);

        ccb.setTextFor(CheckComboBox.MULTIPLE, MidiDeviceManager.getConcatDeviceNames(initialDevices));

        List<MidiDevice> selectedInitialDevices = new ArrayList<>();
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
                midiDeviceManager.setSystemDeviceProvider(selectedDevices, input);

                ccb.setTextFor(CheckComboBox.MULTIPLE, MidiDeviceManager.getConcatDeviceNames(selectedDevices));
            }

        });

        return ccb;
    }

}