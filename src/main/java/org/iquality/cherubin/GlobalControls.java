package org.iquality.cherubin;

import org.japura.gui.CheckComboBox;
import org.japura.gui.event.ListCheckListener;
import org.japura.gui.event.ListEvent;
import org.japura.gui.model.ListCheckModel;

import javax.sound.midi.MidiDevice;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GlobalControls {

    public static void makeToolBar(JToolBar toolBar, AppFrame appFrame) {
        toolBar.add(new JButton(new AbstractAction("All Notes Off") {
            @Override
            public void actionPerformed(ActionEvent e) {
                appFrame.getAppModel().getExecutor().execute(SoundSender::sendAllSoundsOff);
            }
        }));


        CheckComboBox ccb = makeMidiDeviceCombo(true, appFrame.getAppModel().getMidiDeviceManager());
        toolBar.add(ccb);

        ccb = makeMidiDeviceCombo(false, appFrame.getAppModel().getMidiDeviceManager());
        toolBar.add(ccb);

        toolBar.add(Box.createHorizontalStrut(Integer.MAX_VALUE));
    }

    private static CheckComboBox makeMidiDeviceCombo(boolean input, MidiDeviceManager midiDeviceManager) {
        CheckComboBox ccb = new CheckComboBox();
        String dir = input ? "input" : "output";
        ccb.setTextFor(CheckComboBox.NONE, "* no " + dir + " selected *");
        ccb.setTextFor(CheckComboBox.ALL, "* all selected *");

        ListCheckModel ccbModel = ccb.getModel();
        MidiDeviceManager.getAvailableDevices(input).stream().map(MidiDeviceManager.MidiDeviceWrapper::new).forEach(ccbModel::addElement);
        List<MidiDevice> initialDevices = MidiDeviceManager.getInitialDevices(input);

        ccb.setTextFor(CheckComboBox.MULTIPLE, MidiDeviceManager.getConcatDeviceNames(initialDevices));

        List<MidiDeviceManager.MidiDeviceWrapper> initialDeviceWrappers = new ArrayList<>();
        for (int i = 0; i < ccb.getModel().getSize(); i++) {
            MidiDeviceManager.MidiDeviceWrapper devElem = (MidiDeviceManager.MidiDeviceWrapper) ccbModel.getElementAt(i);
            if (initialDevices.contains(devElem.getMidiDevice())) {
                initialDeviceWrappers.add(devElem);
            }
        }
        ccbModel.setCheck(initialDeviceWrappers.toArray());

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
                        map(elem -> ((MidiDeviceManager.MidiDeviceWrapper) elem).getMidiDevice()).
                        collect(Collectors.toList());
                midiDeviceManager.setSystemDeviceProvider(selectedDevices, input);

                ccb.setTextFor(CheckComboBox.MULTIPLE, MidiDeviceManager.getConcatDeviceNames(selectedDevices));
            }

        });

        return ccb;
    }

}