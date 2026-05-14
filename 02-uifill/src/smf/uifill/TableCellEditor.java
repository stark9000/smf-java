package smf.uifill;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Consumer;

/**
 * SMF Library - TableCellEditor
 *
 * Listens for cell edits in a JTable and fires a callback
 * when the user changes a cell value.
 *
 * The callback receives a CellEdit object containing:
 *   - which row and column was edited
 *   - the old value before editing
 *   - the new value after editing
 *
 * This is useful for auto-saving changes back to the database
 * as the user edits the table directly.
 *
 * Usage:
 *   new TableCellEditor(myTable, edit -> {
 *       System.out.println("Row " + edit.getRow() + " changed");
 *       System.out.println("  " + edit.getOldValue() + " → " + edit.getNewValue());
 *       // Save to DB here
 *       db.Update("products",
 *           "price = '" + edit.getNewValue() + "'",
 *           "id = " + getIdForRow(edit.getRow())
 *       );
 *   });
 *
 * Original concept by: stark (originally named jTableEditor)
 * Modernized for Java 8 - Action replaced with Consumer lambda
 */
public class TableCellEditor implements PropertyChangeListener, Runnable {

    // -------------------------------------------------------------------------
    // CellEdit - describes what changed
    // -------------------------------------------------------------------------

    /**
     * Holds information about a single cell edit event.
     */
    public static class CellEdit {

        private final JTable table;
        private final int    row;
        private final int    column;
        private final Object oldValue;
        private final Object newValue;

        CellEdit(JTable table, int row, int column, Object oldValue, Object newValue) {
            this.table    = table;
            this.row      = row;
            this.column   = column;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        /** The table that was edited. */
        public JTable getTable()    { return table; }

        /** The model row index of the edited cell. */
        public int getRow()         { return row; }

        /** The model column index of the edited cell. */
        public int getColumn()      { return column; }

        /** The value before editing. */
        public Object getOldValue() { return oldValue; }

        /** The value after editing. */
        public Object getNewValue() { return newValue; }

        @Override
        public String toString() {
            return "CellEdit[row=" + row + ", col=" + column
                 + ", old=" + oldValue + ", new=" + newValue + "]";
        }
    }

    // -------------------------------------------------------------------------
    // TableCellEditor
    // -------------------------------------------------------------------------

    private final JTable            table;
    private final Consumer<CellEdit> onEdit;  // Java 8 lambda callback

    private int    editingRow;
    private int    editingColumn;
    private Object oldValue;

    /**
     * Attach a cell-edit listener to a JTable.
     *
     * @param table  The JTable to monitor.
     * @param onEdit Lambda called when a cell value changes.
     *               Receives a CellEdit with row, column, old and new values.
     */
    public TableCellEditor(JTable table, Consumer<CellEdit> onEdit) {
        this.table  = table;
        this.onEdit = onEdit;
        this.table.addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if ("tableCellEditor".equals(e.getPropertyName())) {
            if (table.isEditing()) {
                // Editing just started — capture current (old) value
                SwingUtilities.invokeLater(this);
            } else {
                // Editing just stopped — check if value changed
                processEditStopped();
            }
        }
    }

    @Override
    public void run() {
        // Runs after invokeLater — table is now in edit mode
        editingRow    = table.convertRowIndexToModel(table.getEditingRow());
        editingColumn = table.convertColumnIndexToModel(table.getEditingColumn());
        oldValue      = table.getModel().getValueAt(editingRow, editingColumn);
    }

    private void processEditStopped() {
        Object newValue = table.getModel().getValueAt(editingRow, editingColumn);

        // Only fire callback if value actually changed
        if (newValue != null && !newValue.equals(oldValue)) {
            CellEdit edit = new CellEdit(table, editingRow, editingColumn, oldValue, newValue);
            onEdit.accept(edit);
        }
    }
}
