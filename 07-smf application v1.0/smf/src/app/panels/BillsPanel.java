/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package app.panels;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import smf.database.DataStore;
import smf.events.AppEvent;
import smf.events.AppEventBus;
import smf.events.EventWatcher;
import smf.model.BillModel;
import smf.uifill.UIFill;

/**
 *
 * @author saliya
 */
// ============================================================================
// BillsPanel
// ============================================================================
public class BillsPanel extends JPanel implements EventWatcher {

    private final AppEventBus bus = AppEventBus.getInstance();
    private final UIFill ui = new UIFill();
    private JTable table;

    public BillsPanel() {
        setLayout(new BorderLayout(6, 6));
        setBorder(new EmptyBorder(6, 6, 6, 6));

        table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewSelected();
                }
            }
        });

        JButton viewBtn = new JButton("View Invoice");
        JButton refreshBtn = new JButton("Refresh");
        viewBtn.addActionListener(e -> viewSelected());
        refreshBtn.addActionListener(e -> refresh());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        toolbar.add(viewBtn);
        toolbar.add(refreshBtn);
        toolbar.add(new JLabel("  (double-click to view invoice)"));

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("Saved Bills"));
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        add(panel, BorderLayout.CENTER);

        bus.subscribe(this);
        refresh();
    }

    private void viewSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a bill first.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        String inv = table.getModel().getValueAt(modelRow, 1).toString();

        BillModel bill = BillModel.findByInvoiceNumber(inv);
        if (bill == null) {
            JOptionPane.showMessageDialog(this, "Could not load invoice: " + inv);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Invoice:  ").append(bill.getInvoiceNumber()).append("\n");
        sb.append("Customer: ").append(bill.getCustomerName()).append("\n");
        sb.append("Date:     ").append(bill.getPaymentDate()).append("\n");
        sb.append("Status:   ").append(bill.getStatus()).append("\n\n");
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
                "Invoice: " + inv, JOptionPane.INFORMATION_MESSAGE);
    }

    public static String repeat(String str, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, str.charAt(0));
        return new String(chars);
    }

    public void refresh() {
        try (DataStore.CloseableResult cr = BillModel.findAll()) {
            ui.setData(table, cr, true);
        } catch (Exception e) {
            smf.messages.MessageLog.getInstance().error("[BillsPanel] refresh: " + e.getMessage());
        }
    }

    @Override
    public void update(Object event) {
        if (AppEvent.DB_INSERT.equals(event.toString())) {
            SwingUtilities.invokeLater(this::refresh);
        }
    }

    public void unsubscribe() {
        bus.unsubscribe(this);
    }
}
