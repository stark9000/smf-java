package demo;

import smf.model.BillModel;
import smf.model.CustomerModel;
import smf.model.ProductModel;
import smf.events.AppEventBus;
import smf.messages.MessageLog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * SMF Tutorial 05 - Domain Model Demo
 *
 * Shows how to use CustomerModel, ProductModel, and BillModel
 * to build and save domain objects without a database.
 *
 * The domain models are usable standalone — create, populate,
 * call toString() or getTotal() — even without MySQL running.
 * The save() calls will fail gracefully if DB is not connected.
 *
 * What this demo shows:
 *   1. Creating a CustomerModel from a Swing form
 *   2. Creating a ProductModel programmatically
 *   3. Building a BillModel with line items and calculating totals
 *   4. How AbstractPerson eliminates code duplication
 *   5. Domain events firing through AppEventBus on save()
 */
public class DomainModelDemo extends JFrame {

    private final MessageLog  log = MessageLog.getInstance();
    private final AppEventBus bus = AppEventBus.getInstance();

    // Customer form fields
    private JTextField tfName, tfFullName, tfEmail, tfPhone, tfCity, tfCountry;

    // Product display
    private JTextArea productArea;

    // Bill display
    private JTextArea billArea;

    // Log
    private JTextArea logArea;

    // Current objects
    private CustomerModel currentCustomer;
    private BillModel     currentBill;

    public DomainModelDemo() {
        super("SMF Tutorial 05 - Domain Model");
        buildUI();

        // Listen for DB events and refresh log
        bus.subscribe(event -> SwingUtilities.invokeLater(this::refreshLog));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void buildUI() {
        setLayout(new BorderLayout(6, 6));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(8, 8, 8, 8));

        JSplitPane topBottom = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            buildTopPanel(), buildLogPanel());
        topBottom.setDividerLocation(440);
        add(topBottom, BorderLayout.CENTER);
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 8, 0));
        panel.add(buildCustomerPanel());
        panel.add(buildProductPanel());
        panel.add(buildBillPanel());
        return panel;
    }

    // -------------------------------------------------------------------------
    // Panel 1 — Customer form
    // -------------------------------------------------------------------------

    private JPanel buildCustomerPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("1. CustomerModel — extends AbstractPerson"));

        JPanel form = new JPanel(new GridLayout(0, 2, 4, 4));
        form.setBorder(new EmptyBorder(4, 4, 4, 4));

        tfName     = field("John");
        tfFullName = field("John Silva");
        tfEmail    = field("john@example.com");
        tfPhone    = field("+94 77 123 4567");
        tfCity     = field("Kandy");
        tfCountry  = field("Sri Lanka");

        form.add(new JLabel("Name:"));        form.add(tfName);
        form.add(new JLabel("Full Name:"));   form.add(tfFullName);
        form.add(new JLabel("Email:"));       form.add(tfEmail);
        form.add(new JLabel("Phone:"));       form.add(tfPhone);
        form.add(new JLabel("City:"));        form.add(tfCity);
        form.add(new JLabel("Country:"));     form.add(tfCountry);

        JButton createBtn = new JButton("Create Customer");
        createBtn.addActionListener(e -> createCustomer());

        JButton saveBtn = new JButton("Save to DB");
        saveBtn.addActionListener(e -> {
            if (currentCustomer != null) {
                currentCustomer.save();
                refreshLog();
            }
        });

        JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
        buttons.add(createBtn);
        buttons.add(saveBtn);

        panel.add(form,    BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void createCustomer() {
        currentCustomer = new CustomerModel();
        currentCustomer.setName(tfName.getText());
        currentCustomer.setFullName(tfFullName.getText());
        currentCustomer.setEmail(tfEmail.getText());
        currentCustomer.setHandPhoneNumber(tfPhone.getText());
        currentCustomer.setCity(tfCity.getText());
        currentCustomer.setCountry(tfCountry.getText());

        log.info("[Demo] Customer created: " + currentCustomer.getDisplayName());
        log.info("  Email:    " + currentCustomer.getEmail());
        log.info("  Location: " + currentCustomer.getCity() + ", " + currentCustomer.getCountry());
        log.info("  Joined:   " + currentCustomer.getDateJoined());
        log.info("  Active:   " + currentCustomer.isActive());
        refreshLog();
        JOptionPane.showMessageDialog(this,
            "Customer created!\n" + currentCustomer,
            "CustomerModel", JOptionPane.INFORMATION_MESSAGE);
    }

    // -------------------------------------------------------------------------
    // Panel 2 — Product demo
    // -------------------------------------------------------------------------

    private JPanel buildProductPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("2. ProductModel — with calculated fields"));

        productArea = new JTextArea();
        productArea.setEditable(false);
        productArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        productArea.setMargin(new Insets(6, 6, 6, 6));

        JButton createBtn = new JButton("Create Sample Products");
        createBtn.addActionListener(e -> createProducts());

        JButton saveBtn = new JButton("Save to DB");
        saveBtn.addActionListener(e -> {
            ProductModel p = new ProductModel();
            p.setName("ESP32 DevKit");
            p.setCategory("Microcontrollers");
            p.setPurchasePrice(4.50);
            p.setSalesPrice(6.80);
            p.setCurrentStock(50);
            p.setMinStock(10);
            p.save();
            refreshLog();
        });

        JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
        buttons.add(createBtn);
        buttons.add(saveBtn);

        panel.add(new JScrollPane(productArea), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void createProducts() {
        StringBuilder sb = new StringBuilder();

        // Create some sample products
        String[][] data = {
            {"Arduino Nano",   "Microcontrollers", "2.50", "4.50",  "100", "20"},
            {"ESP32 DevKit",   "Microcontrollers", "4.50", "6.80",  "50",  "10"},
            {"STM32F103C8T6",  "Microcontrollers", "2.80", "5.00",  "75",  "15"},
            {"DHT22 Sensor",   "Sensors",          "1.20", "2.50",  "200", "30"},
            {"OLED 0.96\"",    "Displays",         "1.50", "3.20",  "60",  "10"},
            {"WS2812B Strip",  "LEDs",             "3.00", "5.50",  "40",  "5"},
        };

        sb.append(String.format("%-20s %8s %8s %8s %s%n",
            "Name", "Buy", "Sell", "Margin", "Stock"));
        sb.append("-".repeat(55)).append("\n");

        for (String[] d : data) {
            ProductModel p = new ProductModel();
            p.setName(d[0]);
            p.setCategory(d[1]);
            p.setPurchasePrice(Double.parseDouble(d[2]));
            p.setSalesPrice(Double.parseDouble(d[3]));
            p.setCurrentStock(Integer.parseInt(d[4]));
            p.setMinStock(Integer.parseInt(d[5]));

            sb.append(String.format("%-20s $%6.2f $%6.2f $%6.2f  %s%n",
                p.getName(), p.getPurchasePrice(), p.getSalesPrice(),
                p.getMargin(),
                p.isLowStock() ? "LOW!" : "OK"
            ));
        }

        productArea.setText(sb.toString());
        log.info("[Demo] Sample products created and displayed");
        refreshLog();
    }

    // -------------------------------------------------------------------------
    // Panel 3 — Bill builder
    // -------------------------------------------------------------------------

    private JPanel buildBillPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("3. BillModel — line items + calculated total"));

        billArea = new JTextArea();
        billArea.setEditable(false);
        billArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        billArea.setMargin(new Insets(6, 6, 6, 6));

        JButton buildBtn = new JButton("Build Sample Bill");
        buildBtn.addActionListener(e -> buildBill());

        JButton saveBtn = new JButton("Save to DB");
        saveBtn.addActionListener(e -> {
            if (currentBill != null) {
                currentBill.save();
                refreshLog();
            }
        });

        JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
        buttons.add(buildBtn);
        buttons.add(saveBtn);

        panel.add(new JScrollPane(billArea), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void buildBill() {
        currentBill = new BillModel();
        if (currentCustomer != null) {
            currentBill.setCustomerId(currentCustomer.getCustomerId());
            currentBill.setCustomerName(currentCustomer.getName());
        } else {
            currentBill.setCustomerName("Walk-in Customer");
        }

        currentBill.setCurrency("USD");
        currentBill.setDeliveryCharge(2.50);
        currentBill.setTaxRate(0.08);  // 8%

        // Add line items
        currentBill.addItem(1, "Arduino Nano",  3, 4.50);
        currentBill.addItem(2, "ESP32 DevKit",  2, 6.80);
        currentBill.addItem(5, "OLED 0.96\"",   1, 3.20);

        // Display
        StringBuilder sb = new StringBuilder();
        sb.append("Invoice: ").append(currentBill.getInvoiceNumber()).append("\n");
        sb.append("Customer: ").append(currentBill.getCustomerName()).append("\n");
        sb.append("Date: ").append(currentBill.getPaymentDate()).append("\n");
        sb.append("-".repeat(40)).append("\n");

        for (BillModel.BillItem item : currentBill.getItems()) {
            sb.append(String.format("%-18s x%d @ $%.2f = $%.2f%n",
                item.getProductName(), item.getQuantity(),
                item.getUnitPrice(), item.getLineTotal()));
        }

        sb.append("-".repeat(40)).append("\n");
        sb.append(String.format("Subtotal:  $%.2f%n", currentBill.getSubTotal()));
        sb.append(String.format("Tax (8%%):  $%.2f%n", currentBill.getTaxAmount()));
        sb.append(String.format("Delivery:  $%.2f%n", currentBill.getDeliveryCharge()));
        sb.append(String.format("TOTAL:     $%.2f%n", currentBill.getTotal()));
        sb.append("\nStatus: ").append(currentBill.getStatus());

        billArea.setText(sb.toString());
        log.info("[Demo] Bill built: " + currentBill);
        refreshLog();
    }

    // -------------------------------------------------------------------------
    // Log panel
    // -------------------------------------------------------------------------

    private JPanel buildLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("MessageLog — events from domain models"));
        panel.setPreferredSize(new Dimension(900, 160));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        logArea.setBackground(new Color(25, 30, 40));
        logArea.setForeground(new Color(200, 200, 200));

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> { log.clear(); logArea.setText(""); });

        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        panel.add(clearBtn, BorderLayout.EAST);
        return panel;
    }

    private void refreshLog() {
        logArea.setText("");
        log.getAll().forEach(e -> logArea.append(e + "\n"));
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static JTextField field(String defaultText) {
        JTextField tf = new JTextField(defaultText);
        return tf;
    }

    // -------------------------------------------------------------------------
    // Main
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(DomainModelDemo::new);
    }
}
