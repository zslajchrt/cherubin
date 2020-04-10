package org.iquality.cherubin;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class RoutingPanel extends JPanel {

    private final AppModel appModel;

    public RoutingPanel(AppModel appModel) {
        super(new BorderLayout());
        this.appModel = appModel;

        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel rightPanel = new JPanel(new BorderLayout());

        ButtonGroup inputBtnGroup = new ButtonGroup();
        JRadioButton leftInBtn = new JRadioButton("Left In");
        leftInBtn.addChangeListener(e -> {
            if (leftInBtn.isSelected()) {
                appModel.setDefaultInput(AppModel.InputDirection.left);
            }
        });

        JRadioButton rightInBtn = new JRadioButton("Right In");
        rightInBtn.addChangeListener(e -> {
            if (rightInBtn.isSelected()) {
                appModel.setDefaultInput(AppModel.InputDirection.right);
            }
        });

        inputBtnGroup.add(leftInBtn);
        inputBtnGroup.add(rightInBtn);

        inputBtnGroup.setSelected(appModel.getDefaultInputDirection() == AppModel.InputDirection.left ? leftInBtn.getModel() : rightInBtn.getModel(), true);

        JCheckBox leftOutBtn = new JCheckBox("Left out");
        leftOutBtn.setSelected(appModel.getDefaultOutputDirection() == AppModel.OutputDirection.left);

        leftOutBtn.addChangeListener(e -> {
            if (leftOutBtn.isSelected()) {
                appModel.addDefaultOutput(AppModel.OutputDirection.left);
            } else {
                appModel.removeDefaultOutput(AppModel.OutputDirection.left);
            }
        });

        JCheckBox rightOutBtn = new JCheckBox("Right out");
        rightOutBtn.setSelected(appModel.getDefaultOutputDirection() == AppModel.OutputDirection.right);

        rightOutBtn.addChangeListener(e -> {
            if (rightOutBtn.isSelected()) {
                appModel.addDefaultOutput(AppModel.OutputDirection.right);
            } else {
                appModel.removeDefaultOutput(AppModel.OutputDirection.right);
            }
        });

        leftPanel.add(leftInBtn, BorderLayout.PAGE_START);
        leftPanel.add(leftOutBtn, BorderLayout.PAGE_END);

        rightPanel.add(rightInBtn, BorderLayout.PAGE_START);
        rightPanel.add(rightOutBtn, BorderLayout.PAGE_END);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }
}
