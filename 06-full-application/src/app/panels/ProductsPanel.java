package app.panels;

import smf.database.DataStore;
import smf.events.AppEvent;
import smf.events.AppEventBus;
import smf.events.EventWatcher;
import smf.model.ProductModel;
import smf.uifill.UIFill;
import smf.util.AppUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * SMF Full Application - ProductsPanel
 *
 * Displays the products table with:
 *   - Live filter
 *   - Add product form
 *   - Delete selected product
 *   - Auto-refresh when any DB_INSERT / DB_DELETE event fires
 *
 * Implements EventWatcher — reacts to bus events automatically.
 */
public class ProductsPanel extends JPanel implements EventWatcher {

    private final DataStore   db  = DataStore.getInstance();
    private final AppEventBus bus = AppEventBus.getInstance();
    private final UIFill      ui  = new UIFill();

    private JTable     table;
    private JTextField filterField;
    private JTextField tfName, tfCategory, tfBuy, tfSell, tfStock, tfMinStock;

    public ProductsPanel() {
        setLayout(new BorderLayout(6, 6));
        setBorder(new EmptyBorder(6, 6, 6, 6));
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildFormPanel(),  BorderLayout.SOUTH);
        bus.subscribe(this);
        refresh();
    }

    // -------------------------------------------------------------------------
    // Table panel
    // -------------------------------------------------------------------------

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("Products"));

        // Filter bar
        filterField = new JTextField(16);
        JButton filterBtn = new JButton("Filter");
        JButton clearBtn  = new JButton("Clear");
        filterBtn.addActionListener(e -> ui.setFilter(table, filterField.getText()));
        clearBtn.addActionListener(e  -> { filterField.setText(""); ui.setFilter(table, ""); });

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        filterBar.add(new JLabel("Search:"));
        filterBar.add(filterField);
        filterBar.add(filterBtn);
        filterBar.add(clearBtn);

        // Table
        table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Delete button
        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.addActionListener(e -> deleteSelected());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(deleteBtn);

        panel.add(filterBar,              BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(bottom,                 BorderLayout.SOUTH);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Add product form
    // -------------------------------------------------------------------------

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("Add New Product"));

        JPanel fields = new JPanel(new GridLayout(2, 6, 6, 4));

        tfName     = field("ESP32 DevKit");
        tfCategory = field("Microcontrollers");
        tfBuy      = field("4.50");
        tfSell     = field("6.80");
        tfStock    = field("50");
        tfMinStock = field("10");

        fields.add(new JLabel("Name:"));     fields.add(tfName);
        fields.add(new JLabel("Category:")); fields.add(tfCategory);
        fields.add(new JLabel("Buy Price:")); fields.add(tfBuy);
        fields.add(new JLabel("Sell Price:")); fields.add(tfSell);
        fields.add(new JLabel("Stock:"));    fields.add(tfStock);
        fields.add(new JLabel("Min Stock:")); fields.add(tfMinStock);

        JButton addBtn = new JButton("Add Product");
        addBtn.addActionListener(e -> addProduct());

        panel.add(fields, BorderLayout.CENTER);
        panel.add(addBtn, BorderLayout.EAST);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    private void addProduct() {
        try {
            ProductModel p = new ProductModel();
            p.setName(tfName.getText().trim());
            p.setCategory(tfCategory.getText().trim());
            p.setPurchasePrice(Double.parseDouble(tfBuy.getText().trim()));
            p.setSalesPrice(Double.parseDouble(tfSell.getText().trim()));
            p.setCurrentStock(Integer.parseInt(tfStock.getText().trim()));
            p.setMinStock(Integer.parseInt(tfMinStock.getText().trim()));
            p.save();
            // refresh() called automatically via EventWatcher.update()
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Please enter valid numbers for prices and stock.",
                "Input Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a product first.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        Object id = table.getModel().getValueAt(modelRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete product id=" + id + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            ProductModel p = new ProductModel();
            p.setProductId(Integer.parseInt(id.toString()));
            p.delete();
        }
    }

    public void refresh() {
        ui.setData(table, ProductModel.findAll(), true);
    }

    // -------------------------------------------------------------------------
    // EventWatcher — auto-refresh on any DB write event
    // -------------------------------------------------------------------------

    @Override
    public void update(Object event) {
        String evt = event.toString();
        if (AppEvent.DB_INSERT.equals(evt) || AppEvent.DB_DELETE.equals(evt)
                || AppEvent.DB_UPDATE.equals(evt)) {
            SwingUtilities.invokeLater(this::refresh);
        }
    }

    public void unsubscribe() { bus.unsubscribe(this); }

    private static JTextField field(String text) {
        return new JTextField(text, 8);
    }
}
