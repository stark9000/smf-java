package app.panels;

import smf.database.DataStore;
import smf.events.AppEvent;
import smf.events.AppEventBus;
import smf.events.EventWatcher;
import smf.model.BillModel;
import smf.model.CustomerModel;
import smf.model.ProductModel;
import smf.uifill.UIFill;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;

/**
 * SMF Full Application - SalesPanel
 *
 * The main sales screen — this is what the original shop/shade application
 * was building toward:
 *
 *   LEFT:   Products table (from DB, filterable)
 *   CENTER: Cart table (copied rows from products)
 *           Quantity control per item
 *   RIGHT:  Customer selector + Bill summary + Checkout button
 *
 * On checkout:
 *   1. Builds a BillModel from cart items
 *   2. Saves to DB (bills + bill_items tables)
 *   3. Fires DB_INSERT → all panels auto-refresh
 *   4. Shows invoice summary dialog
 *
 * This panel wires together EVERY tutorial piece:
 *   DataStore (01) + UIFill (02) + EventWatcher (03) +
 *   CustomerModel + ProductModel + BillModel (05)
 */
public class SalesPanel extends JPanel implements EventWatcher {

    private final DataStore   db  = DataStore.getInstance();
    private final AppEventBus bus = AppEventBus.getInstance();
    private final UIFill      ui  = new UIFill();

    // Products browser
    private JTable     productsTable;
    private JTextField filterField;

    // Cart
    private JTable           cartTable;
    private DefaultTableModel cartModel;
    private JSpinner          qtySpinner;

    // Customer & totals
    private JComboBox<String> customerCombo;
    private JLabel            subtotalLabel;
    private JLabel            taxLabel;
    private JLabel            totalLabel;
    private JTextField        deliveryField;
    private JTextField        taxRateField;

    public SalesPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(6, 6, 6, 6));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildProductsBrowser(),
            buildRightPanel());
        split.setDividerLocation(420);

        add(split, BorderLayout.CENTER);
        bus.subscribe(this);
        refresh();
    }

    // -------------------------------------------------------------------------
    // Left: Products browser
    // -------------------------------------------------------------------------

    private JPanel buildProductsBrowser() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("Products"));
        panel.setPreferredSize(new Dimension(420, 0));

        filterField = new JTextField(12);
        JButton filterBtn = new JButton("Filter");
        JButton clearBtn  = new JButton("Clear");
        filterBtn.addActionListener(e -> ui.setFilter(productsTable, filterField.getText()));
        clearBtn.addActionListener(e  -> { filterField.setText(""); ui.setFilter(productsTable, ""); });

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        filterBar.add(new JLabel("Search:"));
        filterBar.add(filterField);
        filterBar.add(filterBtn);
        filterBar.add(clearBtn);

        productsTable = new JTable();
        productsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Quantity spinner
        qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        JButton addToCartBtn = new JButton("Add to Cart →");
        addToCartBtn.addActionListener(e -> addToCart());

        JPanel cartBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        cartBar.add(new JLabel("Qty:"));
        cartBar.add(qtySpinner);
        cartBar.add(addToCartBtn);

        panel.add(filterBar,                       BorderLayout.NORTH);
        panel.add(new JScrollPane(productsTable),  BorderLayout.CENTER);
        panel.add(cartBar,                         BorderLayout.SOUTH);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Right panel: cart + checkout
    // -------------------------------------------------------------------------

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));

        panel.add(buildCartPanel(),     BorderLayout.CENTER);
        panel.add(buildCheckoutPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("Cart"));

        // Cart table model — fixed columns
        cartModel = new DefaultTableModel(
            new Object[]{"Product", "Qty", "Unit Price", "Line Total"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        cartTable = new JTable(cartModel);

        JButton removeBtn = new JButton("Remove Selected");
        JButton clearBtn  = new JButton("Clear Cart");
        removeBtn.addActionListener(e -> removeFromCart());
        clearBtn.addActionListener(e  -> clearCart());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        buttons.add(removeBtn);
        buttons.add(clearBtn);

        panel.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        panel.add(buttons,                    BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildCheckoutPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new TitledBorder("Checkout"));

        // Customer selector
        customerCombo = new JComboBox<>();
        customerCombo.addActionListener(e -> recalculate());

        // Pricing inputs
        deliveryField = new JTextField("2.50", 5);
        taxRateField  = new JTextField("8",    4);
        deliveryField.addActionListener(e -> recalculate());
        taxRateField.addActionListener(e  -> recalculate());

        // Totals display
        subtotalLabel = label("$0.00");
        taxLabel      = label("$0.00");
        totalLabel    = label("$0.00");
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 14f));
        totalLabel.setForeground(new Color(0, 120, 0));

        JPanel totals = new JPanel(new GridLayout(0, 2, 6, 4));
        totals.add(new JLabel("Customer:"));   totals.add(customerCombo);
        totals.add(new JLabel("Delivery $:")); totals.add(deliveryField);
        totals.add(new JLabel("Tax %:"));      totals.add(taxRateField);
        totals.add(new JLabel("Subtotal:"));   totals.add(subtotalLabel);
        totals.add(new JLabel("Tax:"));        totals.add(taxLabel);
        totals.add(new JLabel("TOTAL:"));      totals.add(totalLabel);

        JButton checkoutBtn = new JButton("✓ Checkout & Save Bill");
        checkoutBtn.setFont(checkoutBtn.getFont().deriveFont(Font.BOLD, 12f));
        checkoutBtn.setBackground(new Color(0, 120, 0));
        checkoutBtn.setForeground(Color.WHITE);
        checkoutBtn.setOpaque(true);
        checkoutBtn.addActionListener(e -> checkout());

        panel.add(totals,       BorderLayout.CENTER);
        panel.add(checkoutBtn,  BorderLayout.SOUTH);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Cart operations
    // -------------------------------------------------------------------------

    private void addToCart() {
        int row = productsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a product first.");
            return;
        }
        int modelRow = productsTable.convertRowIndexToModel(row);
        Object name  = productsTable.getModel().getValueAt(modelRow, 1); // name column
        Object price = productsTable.getModel().getValueAt(modelRow, 7); // sales_price column
        int qty = (Integer) qtySpinner.getValue();

        double unitPrice = Double.parseDouble(price.toString());
        double lineTotal = qty * unitPrice;

        cartModel.addRow(new Object[]{name, qty, unitPrice, lineTotal});
        recalculate();
    }

    private void removeFromCart() {
        int row = cartTable.getSelectedRow();
        if (row == -1) return;
        cartModel.removeRow(row);
        recalculate();
    }

    private void clearCart() {
        cartModel.setRowCount(0);
        recalculate();
    }

    private void recalculate() {
        double subtotal = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            subtotal += Double.parseDouble(cartModel.getValueAt(i, 3).toString());
        }
        double delivery = parseDouble(deliveryField.getText(), 0);
        double taxPct   = parseDouble(taxRateField.getText(), 0);
        double tax      = subtotal * (taxPct / 100.0);
        double total    = subtotal + tax + delivery;

        subtotalLabel.setText(String.format("$%.2f", subtotal));
        taxLabel.setText(String.format("$%.2f", tax));
        totalLabel.setText(String.format("$%.2f", total));
    }

    // -------------------------------------------------------------------------
    // Checkout
    // -------------------------------------------------------------------------

    private void checkout() {
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Cart is empty.");
            return;
        }

        BillModel bill = new BillModel();

        // Customer
        String customerName = customerCombo.getSelectedItem() != null
            ? customerCombo.getSelectedItem().toString() : "Walk-in";
        bill.setCustomerName(customerName);
        bill.setCurrency("USD");

        double delivery = parseDouble(deliveryField.getText(), 0);
        double taxPct   = parseDouble(taxRateField.getText(), 0);
        bill.setDeliveryCharge(delivery);
        bill.setTaxRate(taxPct / 100.0);

        // Add cart rows as bill items
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            String name  = cartModel.getValueAt(i, 0).toString();
            int    qty   = Integer.parseInt(cartModel.getValueAt(i, 1).toString());
            double price = Double.parseDouble(cartModel.getValueAt(i, 2).toString());
            bill.addItem(-1, name, qty, price);
        }

        // Save
        boolean ok = bill.save();
        if (ok) {
            showInvoice(bill);
            clearCart();
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to save bill. Check DB connection.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showInvoice(BillModel bill) {
        StringBuilder sb = new StringBuilder();
        sb.append("Invoice: ").append(bill.getInvoiceNumber()).append("\n");
        sb.append("Customer: ").append(bill.getCustomerName()).append("\n");
        sb.append("Date: ").append(bill.getPaymentDate()).append("\n\n");
        sb.append(String.format("%-20s %4s %8s %10s%n", "Item", "Qty", "Price", "Total"));
        sb.append("-".repeat(46)).append("\n");
        bill.getItems().forEach(item -> sb.append(String.format(
            "%-20s %4d $%7.2f $%9.2f%n",
            item.getProductName(), item.getQuantity(),
            item.getUnitPrice(), item.getLineTotal())));
        sb.append("-".repeat(46)).append("\n");
        sb.append(String.format("Subtotal:  $%.2f%n", bill.getSubTotal()));
        sb.append(String.format("Tax:       $%.2f%n", bill.getTaxAmount()));
        sb.append(String.format("Delivery:  $%.2f%n", bill.getDeliveryCharge()));
        sb.append(String.format("TOTAL:     $%.2f%n", bill.getTotal()));

        JTextArea area = new JTextArea(sb.toString());
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setEditable(false);
        JOptionPane.showMessageDialog(this,
            new JScrollPane(area), "Invoice — " + bill.getInvoiceNumber(),
            JOptionPane.INFORMATION_MESSAGE);
    }

    // -------------------------------------------------------------------------
    // Refresh
    // -------------------------------------------------------------------------

    public void refresh() {
        ui.setData(productsTable, ProductModel.findAll(), true);

        // Refresh customer combo from DB
        ResultSet rs = CustomerModel.findAll();
        ui.setData(customerCombo, rs, "name");
    }

    @Override
    public void update(Object event) {
        String evt = event.toString();
        if (AppEvent.DB_INSERT.equals(evt) || AppEvent.DB_DELETE.equals(evt)
                || AppEvent.DB_UPDATE.equals(evt)) {
            SwingUtilities.invokeLater(this::refresh);
        }
    }

    public void unsubscribe() { bus.unsubscribe(this); }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return l;
    }

    private static double parseDouble(String s, double fallback) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return fallback; }
    }
}
