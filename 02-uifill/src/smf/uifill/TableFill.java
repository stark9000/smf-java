package smf.uifill;

import javax.swing.JTable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * SMF Library - TableFill
 *
 * Interface for populating a JTable from various data sources:
 *   - A ResultSet directly from the database
 *   - A List of rows
 *   - An ArrayList of ArrayLists (2D structure)
 *
 * Also provides filtering and row-copy utilities.
 *
 * Original concept by: stark (originally named jTableFill)
 * Modernized for Java 8
 */
public interface TableFill {

    /**
     * Fill a JTable from a database ResultSet.
     * Column names are taken automatically from the ResultSet metadata.
     * Optionally enables sorting by clicking column headers.
     *
     * Example:
     *   ResultSet rs = db.Select("products", "*", null, false);
     *   tableFill.setData(table, rs, true);
     *
     * @param table  The JTable to populate.
     * @param rs     ResultSet from a database query.
     * @param sort   If true, enables column sorting on the table.
     */
    void setData(JTable table, ResultSet rs, boolean sort);

    /**
     * Fill a JTable from a List of rows, with explicit column names.
     *
     * @param table       The JTable to populate.
     * @param data        List where each element is one row value.
     * @param columnNames Array of column header names.
     */
    void setData(JTable table, List<Object> data, Object[] columnNames);

    /**
     * Apply a text filter to a JTable.
     * Rows not matching the filter string are hidden (not deleted).
     * Pass an empty string to clear the filter and show all rows.
     *
     * Example:
     *   tableFill.setFilter(table, "Arduino");  // show only rows containing "Arduino"
     *   tableFill.setFilter(table, "");          // show all rows
     *
     * @param table  The JTable to filter.
     * @param filter Regex or plain text to filter by.
     */
    void setFilter(JTable table, String filter);

    /**
     * Copy selected rows from one JTable to another.
     * Duplicate rows (already present in the target) are not added.
     * Works with single selection, multi-selection, and select-all.
     *
     * @param source The JTable to copy rows from.
     * @param target The JTable to copy rows into.
     */
    void copySelectedRows(JTable source, JTable target);

    /**
     * Convert a ResultSet into a 2D ArrayList structure.
     * Useful when you want to hold the data in memory
     * after the ResultSet has been closed.
     *
     * Returns: ArrayList where each element is an ArrayList representing one row.
     * Each inner ArrayList contains the column values as Strings.
     *
     * @param rs The ResultSet to convert.
     * @return 2D ArrayList of String values, or empty list on error.
     */
    ArrayList<ArrayList<Object>> toArrayList(ResultSet rs);
}
