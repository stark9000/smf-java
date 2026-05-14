package app.panels;

import smf.database.DataStore;
import smf.events.AppEvent;
import smf.events.AppEventBus;
import smf.events.EventWatcher;
import smf.model.CustomerModel;
import smf.uifill.UIFill;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * SMF Full Application - CustomersPanel
 *
 * Shows the customers table with add/delete and auto-refresh.
 * Uses CustomerModel (Tutorial 05) directly — no raw SQL in this panel.
 */
public class CustomersPanel extends JPanel implements EventWatcher {

    private final AppEventBus bus = AppEventBus.getInstance();
    private final UIFill      ui  = new UIFill();

    private JTable     table;
    private JTextField tfName, tfFullName, tfEmail, tfPhone, tfCity, tfCountry;

    public CustomersPanel() {
        setLayout(new BorderLayout(6, 6));
        setBorder(new EmptyBorder(6, 6, 6, 6));
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildFormPanel(),  BorderLayout.SOUTH);
        bus.subscribe(this);
        refresh();
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("Customers"));

        table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.addActionListener(e -> deleteSelected());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(deleteBtn);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(bottom,                 BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("Add New Customer"));

        JPanel fields = new JPanel(new GridLayout(2, 6, 6, 4));

        tfName     = field("John");
        tfFullName = field("John Silva");
        tfEmail    = field("john@example.com");
        tfPhone    = field("+94 77 000 0000");
        tfCity     = field("Kandy");
        tfCountry  = field("Sri Lanka");

        fields.add(new JLabel("Name:"));      fields.add(tfName);
        fields.add(new JLabel("Full Name:")); fields.add(tfFullName);
        fields.add(new JLabel("Email:"));     fields.add(tfEmail);
        fields.add(new JLabel("Phone:"));     fields.add(tfPhone);
        fields.add(new JLabel("City:"));      fields.add(tfCity);
        fields.add(new JLabel("Country:"));   fields.add(tfCountry);

        JButton addBtn = new JButton("Add Customer");
        addBtn.addActionListener(e -> addCustomer());

        panel.add(fields, BorderLayout.CENTER);
        panel.add(addBtn, BorderLayout.EAST);
        return panel;
    }

    private void addCustomer() {
        CustomerModel c = new CustomerModel();
        c.setName(tfName.getText().trim());
        c.setFullName(tfFullName.getText().trim());
        c.setEmail(tfEmail.getText().trim());
        c.setHandPhoneNumber(tfPhone.getText().trim());
        c.setCity(tfCity.getText().trim());
        c.setCountry(tfCountry.getText().trim());
        c.save();
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a customer first."); return; }
        int modelRow = table.convertRowIndexToModel(row);
        Object id = table.getModel().getValueAt(modelRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete customer id=" + id + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            CustomerModel c = new CustomerModel();
            c.setCustomerId(Integer.parseInt(id.toString()));
            c.delete();
        }
    }

    public void refresh() {
        ui.setData(table, CustomerModel.findAll(), true);
    }

    @Override
    public void update(Object event) {
        String evt = event.toString();
        if (AppEvent.DB_INSERT.equals(evt) || AppEvent.DB_DELETE.equals(evt)) {
            SwingUtilities.invokeLater(this::refresh);
        }
    }

    public void unsubscribe() { bus.unsubscribe(this); }

    private static JTextField field(String text) { return new JTextField(text, 8); }
}
