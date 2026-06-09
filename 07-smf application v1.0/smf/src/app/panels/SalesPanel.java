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
import java.util.Arrays;

/**
 * SMF Shop - BillsPanel + SalesPanel combined file Both panels fixed to use
 * try-with-resources on CloseableResult.
 */
// ============================================================================
// SalesPanel
// ============================================================================
public class SalesPanel extends JPanel implements EventWatcher {

    private final AppEventBus bus = AppEventBus.getInstance();
    private final UIFill ui = new UIFill();

    private JTable productsTable;
    private JTextField filterField;
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JSpinner qtySpinner;
    private JComboBox<String> customerCombo;
    private JLabel subtotalLabel, taxLabel, totalLabel;
    private JTextField deliveryField, taxRateField;

    public SalesPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(6, 6, 6, 6));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildProductsBrowser(), buildRightPanel());
        split.setDividerLocation(420);
        add(split, BorderLayout.CENTER);

        bus.subscribe(this);
        refresh();
    }

    private JPanel buildProductsBrowser() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("Products"));
        panel.setPreferredSize(new Dimension(420, 0));

        filterField = new JTextField(12);
        JButton fb = new JButton("Filter"), cb = new JButton("Clear");
        fb.addActionListener(e -> ui.setFilter(productsTable, filterField.getText()));
        cb.addActionListener(e -> {
            filterField.setText("");
            ui.setFilter(productsTable, "");
        });

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        filterBar.add(new JLabel("Search:"));
        filterBar.add(filterField);
        filterBar.add(fb);
        filterBar.add(cb);

        productsTable = new JTable();
        productsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));

        JButton addToCart = new JButton("Add to Cart →");
        addToCart.addActionListener(e -> addToCart());

        JPanel cartBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        cartBar.add(new JLabel("Qty:"));
        cartBar.add(qtySpinner);
        cartBar.add(addToCart);

        panel.add(filterBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(productsTable), BorderLayout.CENTER);
        panel.add(cartBar, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(buildCartPanel(), BorderLayout.CENTER);
        panel.add(buildCheckoutPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("Cart"));

        cartModel = new DefaultTableModel(
                new Object[]{"Product", "Qty", "Unit Price", "Line Total"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        cartTable = new JTable(cartModel);

        JButton removeBtn = new JButton("Remove Selected");
        JButton clearBtn = new JButton("Clear Cart");
        removeBtn.addActionListener(e -> removeFromCart());
        clearBtn.addActionListener(e -> clearCart());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        btns.add(removeBtn);
        btns.add(clearBtn);

        panel.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        panel.add(btns, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildCheckoutPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new TitledBorder("Checkout"));

        customerCombo = new JComboBox<>();
        customerCombo.addActionListener(e -> recalculate());
        deliveryField = new JTextField("2.50", 5);
        taxRateField = new JTextField("8", 4);
        deliveryField.addActionListener(e -> recalculate());
        taxRateField.addActionListener(e -> recalculate());

        subtotalLabel = mono("$0.00");
        taxLabel = mono("$0.00");
        totalLabel = mono("$0.00");
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 14f));
        totalLabel.setForeground(new Color(0, 120, 0));

        JPanel totals = new JPanel(new GridLayout(0, 2, 6, 4));
        totals.add(new JLabel("Customer:"));
        totals.add(customerCombo);
        totals.add(new JLabel("Delivery $:"));
        totals.add(deliveryField);
        totals.add(new JLabel("Tax %:"));
        totals.add(taxRateField);
        totals.add(new JLabel("Subtotal:"));
        totals.add(subtotalLabel);
        totals.add(new JLabel("Tax:"));
        totals.add(taxLabel);
        totals.add(new JLabel("TOTAL:"));
        totals.add(totalLabel);

        JButton checkoutBtn = new JButton("✓  Checkout & Save Bill");
        checkoutBtn.setFont(checkoutBtn.getFont().deriveFont(Font.BOLD, 12f));
        checkoutBtn.setBackground(new Color(0, 120, 0));
        checkoutBtn.setForeground(Color.WHITE);
        checkoutBtn.setOpaque(true);
        checkoutBtn.addActionListener(e -> checkout());

        panel.add(totals, BorderLayout.CENTER);
        panel.add(checkoutBtn, BorderLayout.SOUTH);
        return panel;
    }

    private void addToCart() {
        int row = productsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a product first.");
            return;
        }
        int modelRow = productsTable.convertRowIndexToModel(row);
        Object name = productsTable.getModel().getValueAt(modelRow, 1);
        Object price = productsTable.getModel().getValueAt(modelRow, 8); // sales_price index
        int qty = (Integer) qtySpinner.getValue();
        double unitPrice = Double.parseDouble(price.toString());
        cartModel.addRow(new Object[]{name, qty, unitPrice, qty * unitPrice});
        recalculate();
    }

    private void removeFromCart() {
        int row = cartTable.getSelectedRow();
        if (row != -1) {
            cartModel.removeRow(row);
            recalculate();
        }
    }

    private void clearCart() {
        cartModel.setRowCount(0);
        recalculate();
    }

    private void recalculate() {
        double sub = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            sub += Double.parseDouble(cartModel.getValueAt(i, 3).toString());
        }
        double delivery = parseDouble(deliveryField.getText(), 0);
        double taxPct = parseDouble(taxRateField.getText(), 0);
        double tax = sub * (taxPct / 100.0);
        subtotalLabel.setText(String.format("$%.2f", sub));
        taxLabel.setText(String.format("$%.2f", tax));
        totalLabel.setText(String.format("$%.2f", sub + tax + delivery));
    }

    private void checkout() {
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Cart is empty.");
            return;
        }
        BillModel bill = new BillModel();
        bill.setCustomerName(customerCombo.getSelectedItem() != null
                ? customerCombo.getSelectedItem().toString() : "Walk-in");
        bill.setCurrency("USD");
        bill.setDeliveryCharge(parseDouble(deliveryField.getText(), 0));
        bill.setTaxRate(parseDouble(taxRateField.getText(), 0) / 100.0);
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            bill.addItem(-1,
                    cartModel.getValueAt(i, 0).toString(),
                    Integer.parseInt(cartModel.getValueAt(i, 1).toString()),
                    Double.parseDouble(cartModel.getValueAt(i, 2).toString()));
        }
        if (bill.save()) {
            showInvoice(bill);
            clearCart();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Failed to save bill. Check DB connection.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showInvoice(BillModel bill) {
        StringBuilder sb = new StringBuilder();
        sb.append("Invoice: ").append(bill.getInvoiceNumber())
                .append("\nCustomer: ").append(bill.getCustomerName())
                .append("\nDate: ").append(bill.getPaymentDate()).append("\n\n");
        sb.append(String.format("%-20s %4s %8s %10s%n", "Item", "Qty", "Price", "Total"));
        sb.append(repeat("-", 46)).append("\n");
        bill.getItems().forEach(i -> sb.append(String.format(
                "%-20s %4d $%7.2f $%9.2f%n",
                i.getProductName(), i.getQuantity(), i.getUnitPrice(), i.getLineTotal())));
        sb.append(repeat("-", 46)).append("\n");
        sb.append(String.format("Subtotal: $%.2f%nTax:      $%.2f%nDelivery: $%.2f%nTOTAL:    $%.2f%n",
                bill.getSubTotal(), bill.getTaxAmount(), bill.getDeliveryCharge(), bill.getTotal()));
        JTextArea area = new JTextArea(sb.toString());
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "Invoice — " + bill.getInvoiceNumber(), JOptionPane.INFORMATION_MESSAGE);
    }

    // try-with-resources for all CloseableResult usage
    public void refresh() {
        try (DataStore.CloseableResult cr = ProductModel.findAll()) {
            ui.setData(productsTable, cr, true);
        } catch (Exception e) {
            smf.messages.MessageLog.getInstance().error("[SalesPanel] product refresh: " + e.getMessage());
        }
        try (DataStore.CloseableResult cr = CustomerModel.findAll()) {
            ui.setData(customerCombo, cr, "name");
        } catch (Exception e) {
            smf.messages.MessageLog.getInstance().error("[SalesPanel] customer refresh: " + e.getMessage());
        }
    }

    @Override
    public void update(Object event) {
        String evt = event.toString();
        if (AppEvent.DB_INSERT.equals(evt) || AppEvent.DB_DELETE.equals(evt)
                || AppEvent.DB_UPDATE.equals(evt)) {
            SwingUtilities.invokeLater(this::refresh);
        }
    }

    public void unsubscribe() {
        bus.unsubscribe(this);
    }

    private static JLabel mono(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return l;
    }

    private static double parseDouble(String s, double fallback) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public static String repeat(String str, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, str.charAt(0));
        return new String(chars);
    }
}
