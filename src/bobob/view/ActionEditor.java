package bobob.view;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import java.awt.Component;
import java.util.function.IntConsumer;

public class ActionEditor extends DefaultCellEditor {

    private final JButton button = new JButton();
    private final IntConsumer action;
    private int row;

    public ActionEditor(JCheckBox checkBox, IntConsumer action) {
        super(checkBox);
        this.action = action;

        button.addActionListener(e -> {
            fireEditingStopped();
            action.accept(row);
        });
    }

    @Override
    public Component getTableCellEditorComponent(
        JTable table, Object value, boolean selected,
        int row, int column
    ) {
        this.row = table.convertRowIndexToModel(row);
        button.setText(value == null ? "" : value.toString());
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        return button.getText();
    }
}
