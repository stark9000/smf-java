package smf.uifill;

import javax.swing.JList;
import java.sql.ResultSet;
import java.util.List;

/**
 * SMF Library - ListFill
 *
 * Interface for populating a JList from various data sources.
 * Items are automatically sorted alphabetically.
 *
 * Original concept by: stark (originally named jListFill)
 * Modernized for Java 8
 */
public interface ListFill {

    /**
     * Fill a JList from a List of items.
     * Items are sorted alphabetically before being added.
     *
     * Example:
     *   List<Object> items = Arrays.asList("KiCad", "Altium", "EasyEDA");
     *   listFill.setData(list, items);
     *   // Result: Altium, EasyEDA, KiCad
     *
     * @param list The JList to populate.
     * @param data List of items to add.
     */
    void setData(JList<String> list, List<Object> data);

    /**
     * Fill a JList from a single column of a ResultSet.
     *
     * Example:
     *   ResultSet rs = db.Select("suppliers", "name", null, false);
     *   listFill.setData(list, rs, "name");
     *
     * @param list       The JList to populate.
     * @param rs         ResultSet from a database query.
     * @param columnName The column name whose values to use.
     */
    void setData(JList<String> list, ResultSet rs, String columnName);
}
