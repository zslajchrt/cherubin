package org.iquality.cherubin;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

public class GlobalStatusBar extends JPanel {

    private final AppModel appModel;

    public GlobalStatusBar(AppModel appModel) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        this.appModel = appModel;

        int panelSlotHeight = 50;

        MidiActivityPanel midiActivityPanel = new MidiActivityPanel(appModel.getMidiDeviceManager(), appModel.getProxy());

//        JPanel midiActivityPanel = new JPanel(new BorderLayout());
//        midiActivityPanel.add(midiInActivityPanel, BorderLayout.PAGE_START);
//        midiActivityPanel.add(midiOutActivityPanel, BorderLayout.PAGE_END);
        midiActivityPanel.setBorder(BorderFactory.createTitledBorder("MIDI Activity"));
//        midiActivityPanel.setMinimumSize(new Dimension(100, panelSlotHeight));
//        midiActivityPanel.setAlignmentX(RIGHT_ALIGNMENT);

        add(midiActivityPanel);

        JPanel thruPanel = new JPanel(new FlowLayout()) {
            @Override
            public String getToolTipText(MouseEvent event) {
                List<MidiProxy.ProxyLink> links = appModel.getProxy().getLinks();
                StringBuilder sb = new StringBuilder("<html>");
                sb.append("<ol>");
                for (MidiProxy.ProxyLink link : links) {
                    sb.append("<li>").append(link).append("</li>");
                }
                sb.append("</ol></html>");
                return sb.toString();
            }
        };
        thruPanel.setPreferredSize(new Dimension(50, panelSlotHeight));
        thruPanel.setMaximumSize(new Dimension(50, panelSlotHeight));
        thruPanel.setMinimumSize(new Dimension(50, panelSlotHeight));
        thruPanel.setBorder(BorderFactory.createTitledBorder("Thru"));
        JLabel thruLbl = new JLabel() {
            @Override
            public String getToolTipText() {
                List<MidiProxy.ProxyLink> links = appModel.getProxy().getLinks();
                StringBuilder sb = new StringBuilder();
                for (MidiProxy.ProxyLink link : links) {
                    sb.append(link).append('\n');
                }
                return sb.toString();
            }
        };
        thruLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        thruPanel.add(thruLbl);
        thruPanel.setAlignmentX(RIGHT_ALIGNMENT);

        MidiProxy.StatusListener statusListener = getStatusListener(thruLbl);
        appModel.getProxy().addStatusListener(statusListener);

        statusListener.onStatusChanged(appModel.getProxy().isThru());

        add(thruPanel);
    }

    private MidiProxy.StatusListener getStatusListener(JLabel thruLbl) {
        return thru -> {
            thruLbl.setText((thru ? "ON" : "OFF"));
        };
    }
}
