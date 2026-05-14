package smf.uifill;

import javax.swing.JComboBox;
import java.sql.ResultSet;
import java.util.List;

/**
 * SMF Library - ComboFill
 *
 * Interface for populating a JComboBox from various data sources.
 * Items are automatically sorted alphabetically.
 *
 * Original concept by: stark (originally named jComboBoxFill)
 * Modernized for Java 8
 */
public interface ComboFill {

    /**
     * Fill a JComboBox from a List of items.
     * Items are sorted alphabetically before being added.
     * Existing items are cleared first.
     *
     * Example:
     *   List<Object> items = Arrays.asList("ESP32", "Arduino", "STM32");
     *   comboFill.setData(combo, items);
     *   // Result: Arduino, ESP32, STM32
     *
     * @param combo The JComboBox to populate.
     * @param data  List of items to add.
     */
    void setData(JComboBox<String> combo, List<Object> data);

    /**
     * Fill a JComboBox from a single column of a ResultSet.
     * Useful for populating dropdowns directly from a DB query.
     *
     * Example:
     *   ResultSet rs = db.Select("categories", "name", null, false);
     *   comboFill.setData(combo, rs, "name");
     *
     * @param combo      The JComboBox to populate.
     * @param rs         ResultSet from a database query.
     * @param columnName The column name whose values to use.
     */
    void setData(JComboBox<String> combo, ResultSet rs, String columnName);
}
