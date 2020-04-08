package org.iquality.cherubin;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class VirtualBlofeldPanel extends JPanel implements AppExtension {

    public VirtualBlofeldPanel(VirtualBlofeldModel blofeldModel) {
        super(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
        for (int i = 0; i < VirtualBlofeldModel.BANKS_NUMBER; i++) {
            VirtualBlofeldTable blofeldBankTable = new VirtualBlofeldTable(new VirtualBlofeldTableModel(blofeldModel, blofeldModel.getBank(i)));
            tabbedPane.add(new JScrollPane(blofeldBankTable), "" + (char)('A' + i));
        }

        VirtualBlofeldMultiTable blofeldBankTable = new VirtualBlofeldMultiTable(new VirtualBlofeldMultiTableModel(blofeldModel, blofeldModel.getMultiBank()));
        tabbedPane.add(new JScrollPane(blofeldBankTable), "Multi");

        add(tabbedPane, BorderLayout.CENTER);
    }

    @Override
    public String getExtensionName() {
        return "Virtual Blofeld";
    }

    @Override
    public List<Component> getToolbarComponents() {
        return Collections.emptyList();
    }

    @Override
    public void activate() {

    }

    @Override
    public void deactivate() {

    }
}
