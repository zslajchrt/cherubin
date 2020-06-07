package org.iquality.cherubin;

import javax.swing.*;

public interface SoundEditorTableBehavior {

    JTable getJTable();

    int getSoundColumn();

    int getCategoriesColumn();

    boolean isActive();

    void deleteSound(int row);

    void updateSound(Sound sound);

    default void configurePopUpMenu(JPopupMenu popupMenu) {
    }

}
