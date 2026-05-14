package demo;

import smf.database.DataStore;
import smf.uifill.UIFill;
import smf.uifill.TableCellEditor;
import smf.messages.SystemMessages;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * SMF Tutorial 02 - UIFill Demo
 *
 * A complete Swing window demonstrating:
 *   1. Filling a JTable from a database ResultSet (with sort)
 *   2. Filling a JComboBox from a database column
 *   3. Filling a JList from a database column
 *   4. Live text filtering on JTable
 *   5. Copying selected rows from one JTable to another
 *   6. Detecting cell edits and saving back to DB
 *   7. Filling components from plain Java Lists (no DB needed)
 *
 * Run this class to see the demo window.
 *
 * To use with a real database:
 *   - Configure DB settings in setupDatabase()
 *   - Create the test table shown in README.md
 *   - Click "Load from DB" buttons
 *
 * To run without a database:
 *   - Click "Load from List" buttons — uses hardcoded sample data
 */
public class UIFillDemo extends JFrame {

    // --- SMF components ---
    private final DataStore db  = DataStore.getInstance();
    private final UIFill    ui  = new UIFill();

    // --- Swing components ---
    private JTable     mainTable;
    private JTable     cartTable;
    private JComboBox<String> categoryCombo;
    private JList<String>     supplierList;
    private JTextField filterField;
    private JTextArea  logArea;

    public UIFillDemo() {
        super("SMF Tutorial 02 - UIFill Demo");
        setupDatabase();
        buildUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // -------------------------------------------------------------------------
    // Database setup
    // -------------------------------------------------------------------------

    private void setupDatabase() {
        db.setServerIP("127.0.0.1");
        db.setServerPort("3306");
        db.setDatabaseName("smf_test");
        db.setDatabaseUser("root");
        db.setDatabasePassword("yourpassword");

        // Listen to DB events and show them in the log
        db.registerWatcher(event -> log("DB event: " + event));
    }

    // -------------------------------------------------------------------------
    // UI Construction
    // -------------------------------------------------------------------------

    private void buildUI() {
        setLayout(new BorderLayout(5, 5));

        add(buildTopPanel(),    BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildLogPanel(),    BorderLayout.SOUTH);
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Controls"));

        // --- Filter ---
        filterField = new JTextField(12);
        JButton filterBtn = new JButton("Filter Table");
        filterBtn.addActionListener(e -> {
            ui.setFilter(mainTable, filterField.getText());
            log("Filter applied: '" + filterField.getText() + "'");
        });

        JButton clearFilterBtn = new JButton("Clear Filter");
        clearFilterBtn.addActionListener(e -> {
            filterField.setText("");
            ui.setFilter(mainTable, "");
            log("Filter cleared");
        });

        // --- Copy rows ---
        JButton copyBtn = new JButton("Copy Selected → Cart");
        copyBtn.addActionListener(e -> {
            ui.copySelectedRows(mainTable, cartTable);
            log("Rows copied to cart table");
        });

        // --- Load from List (no DB needed) ---
        JButton loadListBtn = new JButton("Load from List");
        loadListBtn.addActionListener(e -> loadFromLists());

        // --- Load from DB ---
        JButton loadDbBtn = new JButton("Load from DB");
        loadDbBtn.addActionListener(e -> loadFromDatabase());

        // --- Show log messages ---
        JButton showLogBtn = new JButton("Show System Messages");
        showLogBtn.addActionListener(e -> {
            logArea.setText("");
            SystemMessages.getInstance().getMessages().forEach(m -> logArea.append(m + "\n"));
        });

        panel.add(new JLabel("Filter:"));
        panel.add(filterField);
        panel.add(filterBtn);
        panel.add(clearFilterBtn);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(copyBtn);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(loadListBtn);
        panel.add(loadDbBtn);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(showLogBtn);

        return panel;
    }

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 5, 5));

        // --- Left: main table + cart table ---
        JPanel tablesPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        mainTable = new JTable();
        JScrollPane mainScroll = new JScrollPane(mainTable);
        mainScroll.setBorder(BorderFactory.createTitledBorder("Products (JTable + Filter + Sort)"));
        tablesPanel.add(mainScroll);

        // Attach cell edit listener to mainTable
        // Fires when the user edits a cell directly in the table
        new TableCellEditor(mainTable, edit -> {
            log("Cell edited — Row: " + edit.getRow()
              + "  Col: " + edit.getColumn()
              + "  Old: " + edit.getOldValue()
              + "  New: " + edit.getNewValue());
            // In a real app, save back to DB here:
            // db.Update("products", "price = '" + edit.getNewValue() + "'",
            //           "id = " + getIdForRow(edit.getRow()));
        });

        cartTable = new JTable();
        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartScroll.setBorder(BorderFactory.createTitledBorder("Cart (copied rows)"));
        tablesPanel.add(cartScroll);

        panel.add(tablesPanel);

        // --- Middle: ComboBox ---
        JPanel comboPanel = new JPanel(new BorderLayout());
        comboPanel.setBorder(BorderFactory.createTitledBorder("Categories (JComboBox)"));
        categoryCombo = new JComboBox<>();
        categoryCombo.addActionListener(e ->
            log("ComboBox selected: " + categoryCombo.getSelectedItem()));
        comboPanel.add(categoryCombo, BorderLayout.NORTH);
        comboPanel.add(new JLabel("<html><center>Select a category<br>above to filter</center></html>",
                       SwingConstants.CENTER), BorderLayout.CENTER);
        panel.add(comboPanel);

        // --- Right: JList ---
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBorder(BorderFactory.createTitledBorder("Suppliers (JList)"));
        supplierList = new JList<>();
        supplierList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                log("List selected: " + supplierList.getSelectedValue());
            }
        });
        listPanel.add(new JScrollPane(supplierList), BorderLayout.CENTER);
        panel.add(listPanel);

        return panel;
    }

    private JPanel buildLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Event Log"));
        panel.setPreferredSize(new Dimension(900, 120));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        return panel;
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    /**
     * Load sample data from plain Java Lists.
     * No database required — good for testing the UI layer.
     */
    private void loadFromLists() {
        log("--- Loading from Lists ---");

        // Fill JTable with column names + data
        ui.setData(mainTable,
            Arrays.asList("Arduino Nano", "ESP32", "STM32F103", "ATmega328P", "ESP8266"),
            new Object[]{"Product Name"}
        );

        // Fill JComboBox
        ui.setData(categoryCombo,
            Arrays.asList("Microcontrollers", "Sensors", "Displays", "Power", "RF Modules")
        );

        // Fill JList
        ui.setData(supplierList,
            Arrays.asList("DigiKey", "Mouser", "LCSC", "AliExpress", "RS Components")
        );

        log("All components filled from Lists");
    }

    /**
     * Load data from the database.
     * Requires MySQL running with the smf_test database.
     * See README.md for the CREATE TABLE statements.
     */
    private void loadFromDatabase() {
        log("--- Loading from Database ---");

        // Fill main JTable from DB — auto column names, with sorting
        ui.setData(mainTable,
            db.Select("products", "*", null, false),
            true
        );

        // Fill ComboBox from a single DB column
        ui.setData(categoryCombo,
            db.Select("categories", "name", null, false),
            "name"
        );

        // Fill JList from a single DB column
        ui.setData(supplierList,
            db.Select("suppliers", "name", null, false),
            "name"
        );

        log("All components filled from Database");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // -------------------------------------------------------------------------
    // Main
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        // Use system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(UIFillDemo::new);
    }
}
