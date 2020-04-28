package org.iquality.cherubin;

import javax.sound.midi.MidiDevice;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.function.Consumer;

public class MidiThruDialog extends CommonDialog {

    private final ProxyLinkListModel proxyLinkListModel;

    public MidiThruDialog(JFrame parent, List<MidiProxy.ProxyLink> links, Consumer<List<MidiProxy.ProxyLink>> resultConsumer) {
        super(parent, "MIDI Thru Configuration", true);

        if (parent != null) {
            Dimension parentSize = parent.getSize();
            Point p = parent.getLocation();
            setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        }
        JPanel mainPane = new JPanel();
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.Y_AXIS));

        JLabel midiInLbl = adjust(new JLabel("MIDI In:"));
        mainPane.add(midiInLbl);
        JComboBox<MidiDevice> inCombo = new JComboBox<>(new Vector<>(MidiDeviceManager.getAvailableDevices(true)));
        midiInLbl.setLabelFor(inCombo);
        mainPane.add(adjust(inCombo));

        JLabel midiOutLbl = adjust(new JLabel("MIDI Out:"));
        mainPane.add(midiOutLbl);
        JComboBox<MidiDevice> outCombo = new JComboBox<>(new Vector<>(MidiDeviceManager.getAvailableDevices(false)));
        midiOutLbl.setLabelFor(outCombo);
        mainPane.add(adjust(outCombo));

        mainPane.add(adjust(new JLabel("Links:")));
        proxyLinkListModel = new ProxyLinkListModel(links);
        JList<MidiProxy.ProxyLink> linksList = new JList<>(proxyLinkListModel);
        mainPane.add(adjust(new JScrollPane(linksList)));

        mainPane.setBorder(new EmptyBorder(5, 5, 5, 5));

        getContentPane().add(mainPane);

        JPanel buttonPane = new JPanel();

        JButton addOK = new JButton(new AbstractAction("Add") {
            @Override
            public void actionPerformed(ActionEvent e) {
                MidiDeviceManager.MidiDeviceWrapper inDeviceWrapper = (MidiDeviceManager.MidiDeviceWrapper) inCombo.getSelectedItem();
                if (inDeviceWrapper == null) {
                    JOptionPane.showMessageDialog(null, "Select an input first.", "No input selected", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                MidiDeviceManager.MidiDeviceWrapper outDeviceWrapper = (MidiDeviceManager.MidiDeviceWrapper) outCombo.getSelectedItem();
                if (outDeviceWrapper == null) {
                    JOptionPane.showMessageDialog(null, "Select an output first.", "No output selected", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                ProxyLinkListModel model = (ProxyLinkListModel) linksList.getModel();
                model.add(new MidiProxy.ProxyLink(inDeviceWrapper, outDeviceWrapper, null));
            }
        });

        buttonPane.add(addOK);

        JButton removeOK = new JButton(new AbstractAction("Remove") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = linksList.getSelectedIndex();
                if (selectedIndex < 0) {
                    JOptionPane.showMessageDialog(null, "Select a link first.", "No link selected", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                ProxyLinkListModel model = (ProxyLinkListModel) linksList.getModel();
                if (model.getSize() > 0) {
                    model.remove(selectedIndex);
                    if (model.getSize() > 0) {
                        linksList.setSelectedIndex(0);
                    }
                }
            }
        });
        buttonPane.add(removeOK);

        JButton btnOK = new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();

                resultConsumer.accept(getLinks());
            }
        });
        buttonPane.add(btnOK);

        JButton btnCancel = new JButton(new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });
        buttonPane.add(btnCancel);

        getRootPane().setDefaultButton(btnOK);

        getContentPane().add(buttonPane, BorderLayout.SOUTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        installEscapeCloseOperation();

        pack();
        setVisible(true);
    }

    public List<MidiProxy.ProxyLink> getLinks() {
        return new ArrayList<>(proxyLinkListModel.links);
    }

    private <T extends JComponent> T adjust(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    static class ProxyLinkListModel extends AbstractListModel<MidiProxy.ProxyLink> {

        private final Vector<MidiProxy.ProxyLink> links;

        public ProxyLinkListModel(List<MidiProxy.ProxyLink> links) {
            this.links = new Vector<>(links);
        }

        public int getSize() {
            return links.size();
        }

        public MidiProxy.ProxyLink getElementAt(int i) {
            return links.elementAt(i);
        }

        void remove(int index) {
            links.remove(index);
            fireContentsChanged(this, 0, 0);
        }

        void add(MidiProxy.ProxyLink newLink) {
            links.add(newLink);
            fireContentsChanged(this, links.size() - 1, links.size() - 1);
        }

    }

    public static void main(String[] a) {
        List<MidiProxy.ProxyLink> links = new ArrayList<>();
        links.add(new MidiProxy.ProxyLink(MidiDeviceManager.findDevice("Blofeld", true), MidiDeviceManager.findDevice("Blofeld", false), null));
        links.add(new MidiProxy.ProxyLink(MidiDeviceManager.findDevice("Peak", true), MidiDeviceManager.findDevice("Peak", false), null));
        new MidiThruDialog(new JFrame(), links, (result) -> result.forEach(System.out::println));
    }

}