package smf.uifill;

import smf.messages.SystemMessages;

import javax.swing.*;
import javax.swing.table.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

/**
 * SMF Library - UIFill
 *
 * The main implementation of TableFill, ComboFill, and ListFill.
 *
 * One class that handles populating JTable, JComboBox, and JList
 * components directly from database ResultSets or Java Lists.
 *
 * Designed to be used as a mixin — your JFrame can implement
 * TableFill, ComboFill, ListFill and delegate to this class,
 * or simply instantiate UIFill and call its methods directly.
 *
 * Direct usage example:
 *   UIFill ui = new UIFill();
 *   ui.setData(myTable, db.Select("products", "*", null, false), true);
 *   ui.setData(myCombo, db.Select("categories", "name", null, false), "name");
 *   ui.setFilter(myTable, "Arduino");
 *
 * Original concept by: stark
 * Modernized for Java 8 — generics, streams, lambdas, try-with-resources
 */
public class UIFill implements TableFill, ComboFill, ListFill {

    private final SystemMessages log = SystemMessages.getInstance();

    // =========================================================================
    // TableFill implementation
    // =========================================================================

    /**
     * Fill a JTable from a ResultSet.
     * Column names are read automatically from ResultSet metadata.
     * Existing table data is cleared before filling.
     *
     * The table model is set to non-editable by default.
     * Use TableCellEditor separately if you need editable cells.
     */
    @Override
    public void setData(JTable table, ResultSet rs, boolean sort) {
        if (rs == null) {
            log.addMessage("[UIFill] setData(JTable, ResultSet): ResultSet is null");
            return;
        }

        try {
            ResultSetMetaData meta   = rs.getMetaData();
            int columnCount          = meta.getColumnCount();

            // Build column names from metadata
            String[] columnNames = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                columnNames[i] = meta.getColumnLabel(i + 1);
            }

            // Build rows
            ArrayList<Object[]> rows = new ArrayList<>();
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                rows.add(row);
            }

            // Build non-editable DefaultTableModel
            DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            // Add all rows
            rows.forEach(model::addRow);

            // Apply model to table on EDT
            SwingUtilities.invokeLater(() -> {
                table.setModel(model);
                if (sort) {
                    table.setRowSorter(new TableRowSorter<>(model));
                }
            });

            log.addMessage("[UIFill] JTable filled: " + rows.size()
                         + " rows, " + columnCount + " columns");

        } catch (Exception e) {
            log.addMessage("[UIFill] setData(JTable, ResultSet) failed: " + e.getMessage());
        }
    }

    /**
     * Fill a JTable from a List with explicit column names.
     * Each element in data becomes one row in the first column.
     */
    @Override
    public void setData(JTable table, List<Object> data, Object[] columnNames) {
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        if (data != null) {
            data.forEach(item -> model.addRow(new Object[]{item}));
        }

        SwingUtilities.invokeLater(() -> table.setModel(model));
        log.addMessage("[UIFill] JTable filled from List: "
                     + (data != null ? data.size() : 0) + " rows");
    }

    /**
     * Apply a text filter to a JTable.
     * Uses regex — plain text also works as a substring match.
     * Pass "" to clear the filter and show all rows.
     */
    @Override
    public void setFilter(JTable table, String filter) {
        TableModel model = table.getModel();
        if (!(model instanceof DefaultTableModel)) {
            log.addMessage("[UIFill] setFilter: table model is not a DefaultTableModel");
            return;
        }

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        if (filter == null || filter.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            try {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + filter));
            } catch (Exception e) {
                log.addMessage("[UIFill] setFilter invalid regex: " + e.getMessage());
            }
        }
    }

    /**
     * Copy selected rows from source JTable to target JTable.
     * Handles: single row, multiple selected rows, all rows selected.
     * Duplicate rows (already in target) are skipped.
     */
    @Override
    public void copySelectedRows(JTable source, JTable target) {
        DefaultTableModel sourceModel = (DefaultTableModel) source.getModel();
        DefaultTableModel targetModel = (DefaultTableModel) target.getModel();

        int[] selectedRows = source.getSelectedRows();
        if (selectedRows.length == 0) {
            log.addMessage("[UIFill] copySelectedRows: no rows selected");
            return;
        }

        for (int viewRow : selectedRows) {
            // Convert view index to model index (important when sorting is active)
            int modelRow = source.convertRowIndexToModel(viewRow);
            Object rowData = sourceModel.getDataVector().get(modelRow);

            // Skip duplicates
            if (!targetModel.getDataVector().contains(rowData)) {
                targetModel.getDataVector().add(rowData);
            }
        }

        // Refresh the target table
        SwingUtilities.invokeLater(() -> {
            targetModel.fireTableStructureChanged();
            target.setRowSorter(new TableRowSorter<>(targetModel));
        });

        log.addMessage("[UIFill] copySelectedRows: copied " + selectedRows.length + " rows");
    }

    /**
     * Convert a ResultSet to a 2D ArrayList.
     * Useful for holding data in memory after the ResultSet is closed.
     * Each inner list is one row, containing column values as Strings.
     */
    @Override
    public ArrayList<ArrayList<Object>> toArrayList(ResultSet rs) {
        ArrayList<ArrayList<Object>> table = new ArrayList<>();

        if (rs == null) return table;

        try {
            int columnCount = rs.getMetaData().getColumnCount();

            while (rs.next()) {
                ArrayList<Object> row = new ArrayList<>(columnCount);
                for (int c = 1; c <= columnCount; c++) {
                    String val = rs.getString(c);
                    row.add(val != null ? val : "");
                }
                table.add(row);
            }
        } catch (Exception e) {
            log.addMessage("[UIFill] toArrayList failed: " + e.getMessage());
        }

        return table;
    }

    // =========================================================================
    // ComboFill implementation
    // =========================================================================

    /**
     * Fill a JComboBox from a List.
     * Items are sorted alphabetically. Existing items are cleared first.
     */
    @Override
    public void setData(JComboBox<String> combo, List<Object> data) {
        combo.removeAllItems();

        if (data == null || data.isEmpty()) return;

        // Sort alphabetically using TreeSet
        TreeSet<String> sorted = new TreeSet<>();
        data.forEach(item -> sorted.add(item.toString()));

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        sorted.forEach(model::addElement);

        SwingUtilities.invokeLater(() -> combo.setModel(model));
        log.addMessage("[UIFill] JComboBox filled: " + sorted.size() + " items");
    }

    /**
     * Fill a JComboBox from a single column of a ResultSet.
     */
    @Override
    public void setData(JComboBox<String> combo, ResultSet rs, String columnName) {
        if (rs == null) return;

        List<Object> items = new ArrayList<>();
        try {
            while (rs.next()) {
                String val = rs.getString(columnName);
                if (val != null) items.add(val);
            }
        } catch (Exception e) {
            log.addMessage("[UIFill] setData(JComboBox, ResultSet) failed: " + e.getMessage());
        }

        setData(combo, items);
    }

    // =========================================================================
    // ListFill implementation
    // =========================================================================

    /**
     * Fill a JList from a List.
     * Items are sorted alphabetically.
     */
    @Override
    public void setData(JList<String> list, List<Object> data) {
        DefaultListModel<String> model = new DefaultListModel<>();

        if (data != null && !data.isEmpty()) {
            TreeSet<String> sorted = new TreeSet<>();
            data.forEach(item -> sorted.add(item.toString()));
            sorted.forEach(model::addElement);
        }

        SwingUtilities.invokeLater(() -> list.setModel(model));
        log.addMessage("[UIFill] JList filled: " + model.getSize() + " items");
    }

    /**
     * Fill a JList from a single column of a ResultSet.
     */
    @Override
    public void setData(JList<String> list, ResultSet rs, String columnName) {
        if (rs == null) return;

        List<Object> items = new ArrayList<>();
        try {
            while (rs.next()) {
                String val = rs.getString(columnName);
                if (val != null) items.add(val);
            }
        } catch (Exception e) {
            log.addMessage("[UIFill] setData(JList, ResultSet) failed: " + e.getMessage());
        }

        setData(list, items);
    }
}
