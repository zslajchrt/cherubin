package org.iquality.cherubin;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public class LambdaAction extends AbstractAction {

    private final Consumer<ActionEvent> action;

    public LambdaAction(Consumer<ActionEvent> action) {
        this.action = action;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        action.accept(e);
    }
}
