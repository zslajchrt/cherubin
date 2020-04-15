package org.iquality.cherubin.test;

import com.jhe.hexed.JHexEditor;

import java.awt.Component;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

public class Main {
    public static void main(String[] argv) throws Exception {
        JTable table = new JTable(new DefaultTableModel(new Object[][] {{"a1", "a1"}, {"b1", "b2"}}, new Object[] {"c1", "c2"}));

        TableColumn col = table.getColumnModel().getColumn(0);
        col.setCellEditor(new MyTableCellEditor());

        JFrame win=new JFrame();
        win.getContentPane().add(table);
        win.pack();
        win.show();

    }
}

class MyTableCellEditor extends AbstractCellEditor implements TableCellEditor {

    JComponent component = new JTextField();

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                 int rowIndex, int vColIndex) {

        ((JTextField) component).setText((String) value);

        return component;
    }

    public Object getCellEditorValue() {
        return ((JTextField) component).getText();
    }
}
