package smf.uifill;

import smf.database.DataStore;
import smf.messages.MessageLog;

import javax.swing.*;
import javax.swing.table.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * SMF - UIFill
 *
 * Populates JTable, JComboBox, and JList from database ResultSets or Lists.
 *
 * Fixes vs tutorial version:
 *   - setData(JTable, CloseableResult) — caller passes CloseableResult so
 *     the Statement is closed after reading, not leaked
 *   - setData(JComboBox, CloseableResult, column) — same pattern
 *   - setData(JList,     CloseableResult, column) — same pattern
 *   - All ResultSet iteration wrapped in try/catch with proper logging
 *   - Overloads kept for plain ResultSet where the caller manages lifecycle
 */
public class UIFill {

    private final MessageLog log = MessageLog.getInstance();

    // =========================================================================
    // JTable
    // =========================================================================

    /**
     * Fill a JTable from a CloseableResult (preferred — closes Statement automatically).
     *
     * Usage:
     *   try (DataStore.CloseableResult cr = db.query("SELECT * FROM products")) {
     *       ui.setData(table, cr, true);
     *   }
     */
    public void setData(JTable table, DataStore.CloseableResult cr, boolean sort) {
        if (cr == null) return;
        setData(table, cr.getResultSet(), sort);
    }

    /**
     * Fill a JTable from a ResultSet.
     * Column names are read from ResultSet metadata automatically.
     */
    public void setData(JTable table, ResultSet rs, boolean sort) {
        if (rs == null) {
            log.warn("[UIFill] setData(JTable): ResultSet is null");
            return;
        }
        try {
            ResultSetMetaData meta  = rs.getMetaData();
            int columnCount         = meta.getColumnCount();

            String[] columnNames = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                columnNames[i] = meta.getColumnLabel(i + 1);
            }

            List<Object[]> rows = new ArrayList<>();
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                rows.add(row);
            }

            DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            rows.forEach(model::addRow);

            SwingUtilities.invokeLater(() -> {
                table.setModel(model);
                if (sort) table.setRowSorter(new TableRowSorter<>(model));
            });

            log.info("[UIFill] JTable filled: " + rows.size() + " rows");

        } catch (SQLException e) {
            log.error("[UIFill] setData(JTable) failed: " + e.getMessage());
        }
    }

    /**
     * Fill a JTable from a List with explicit column names.
     */
    public void setData(JTable table, List<Object> data, Object[] columnNames) {
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        if (data != null) data.forEach(item -> model.addRow(new Object[]{item}));
        SwingUtilities.invokeLater(() -> table.setModel(model));
    }

    /**
     * Apply a text filter (regex, case-insensitive). Pass "" to clear.
     */
    public void setFilter(JTable table, String filter) {
        TableModel model = table.getModel();
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        if (filter == null || filter.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            try {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + filter));
            } catch (Exception e) {
                log.warn("[UIFill] setFilter invalid regex: " + e.getMessage());
            }
        }
    }

    /**
     * Copy selected rows from source to target. Skips duplicates.
     */
    public void copySelectedRows(JTable source, JTable target) {
        if (!(target.getModel() instanceof DefaultTableModel)) return;
        DefaultTableModel targetModel = (DefaultTableModel) target.getModel();

        int[] selectedRows = source.getSelectedRows();
        for (int viewRow : selectedRows) {
            int modelRow = source.convertRowIndexToModel(viewRow);
            Object rowData = ((DefaultTableModel) source.getModel())
                .getDataVector().get(modelRow);
            if (!targetModel.getDataVector().contains(rowData)) {
                targetModel.getDataVector().add(rowData);
            }
        }
        SwingUtilities.invokeLater(() -> {
            targetModel.fireTableStructureChanged();
            target.setRowSorter(new TableRowSorter<>(targetModel));
        });
    }

    // =========================================================================
    // JComboBox
    // =========================================================================

    /**
     * Fill a JComboBox from a CloseableResult (preferred).
     */
    public void setData(JComboBox<String> combo, DataStore.CloseableResult cr, String col) {
        if (cr == null) return;
        setData(combo, cr.getResultSet(), col);
    }

    /**
     * Fill a JComboBox from a ResultSet column. Items are sorted alphabetically.
     */
    public void setData(JComboBox<String> combo, ResultSet rs, String columnName) {
        if (rs == null) return;
        List<Object> items = new ArrayList<>();
        try {
            while (rs.next()) {
                String val = rs.getString(columnName);
                if (val != null) items.add(val);
            }
        } catch (SQLException e) {
            log.error("[UIFill] setData(JComboBox) failed: " + e.getMessage());
        }
        setData(combo, items);
    }

    /**
     * Fill a JComboBox from a List. Items are sorted alphabetically.
     */
    public void setData(JComboBox<String> combo, List<Object> data) {
        combo.removeAllItems();
        if (data == null || data.isEmpty()) return;

        TreeSet<String> sorted = new TreeSet<>();
        data.forEach(item -> sorted.add(item.toString()));

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        sorted.forEach(model::addElement);
        SwingUtilities.invokeLater(() -> combo.setModel(model));
    }

    // =========================================================================
    // JList
    // =========================================================================

    /**
     * Fill a JList from a CloseableResult (preferred).
     */
    public void setData(JList<String> list, DataStore.CloseableResult cr, String col) {
        if (cr == null) return;
        setData(list, cr.getResultSet(), col);
    }

    /**
     * Fill a JList from a ResultSet column. Items are sorted alphabetically.
     */
    public void setData(JList<String> list, ResultSet rs, String columnName) {
        if (rs == null) return;
        List<Object> items = new ArrayList<>();
        try {
            while (rs.next()) {
                String val = rs.getString(columnName);
                if (val != null) items.add(val);
            }
        } catch (SQLException e) {
            log.error("[UIFill] setData(JList) failed: " + e.getMessage());
        }
        setData(list, items);
    }

    /**
     * Fill a JList from a List. Items are sorted alphabetically.
     */
    public void setData(JList<String> list, List<Object> data) {
        DefaultListModel<String> model = new DefaultListModel<>();
        if (data != null) {
            TreeSet<String> sorted = new TreeSet<>();
            data.forEach(item -> sorted.add(item.toString()));
            sorted.forEach(model::addElement);
        }
        SwingUtilities.invokeLater(() -> list.setModel(model));
    }
}
