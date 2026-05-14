package app.panels;

import smf.events.AppEvent;
import smf.events.AppEventBus;
import smf.events.EventWatcher;
import smf.model.BillModel;
import smf.uifill.UIFill;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * SMF Full Application - BillsPanel
 *
 * Shows all saved bills.
 * Double-click any row to view the full invoice.
 * Auto-refreshes when a new bill is saved (DB_INSERT event).
 */
public class BillsPanel extends JPanel implements EventWatcher {

    private final AppEventBus bus = AppEventBus.getInstance();
    private final UIFill      ui  = new UIFill();
    private JTable table;

    public BillsPanel() {
        setLayout(new BorderLayout(6, 6));
        setBorder(new EmptyBorder(6, 6, 6, 6));

        table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Double-click to view invoice
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) viewSelected();
            }
        });

        JButton viewBtn    = new JButton("View Invoice");
        JButton refreshBtn = new JButton("Refresh");
        viewBtn.addActionListener(e    -> viewSelected());
        refreshBtn.addActionListener(e -> refresh());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        toolbar.add(viewBtn);
        toolbar.add(refreshBtn);
        toolbar.add(new JLabel("  (double-click a row to view invoice)"));

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("Saved Bills"));
        panel.add(toolbar,                    BorderLayout.NORTH);
        panel.add(new JScrollPane(table),     BorderLayout.CENTER);

        add(panel, BorderLayout.CENTER);
        bus.subscribe(this);
        refresh();
    }

    private void viewSelected() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a bill first."); return; }
        int modelRow = table.convertRowIndexToModel(row);
        String invoiceNumber = table.getModel().getValueAt(modelRow, 1).toString();

        BillModel bill = BillModel.findByInvoiceNumber(invoiceNumber);
        if (bill == null) {
            JOptionPane.showMessageDialog(this, "Could not load invoice: " + invoiceNumber);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Invoice:  ").append(bill.getInvoiceNumber()).append("\n");
        sb.append("Customer: ").append(bill.getCustomerName()).append("\n");
        sb.append("Date:     ").append(bill.getPaymentDate()).append("\n");
        sb.append("Status:   ").append(bill.getStatus()).append("\n\n");
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
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
            "Invoice: " + invoiceNumber, JOptionPane.INFORMATION_MESSAGE);
    }

    public void refresh() {
        ui.setData(table, BillModel.findAll(), true);
    }

    @Override
    public void update(Object event) {
        if (AppEvent.DB_INSERT.equals(event.toString())) {
            SwingUtilities.invokeLater(this::refresh);
        }
    }

    public void unsubscribe() { bus.unsubscribe(this); }
}
