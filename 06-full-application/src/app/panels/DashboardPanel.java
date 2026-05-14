package app.panels;

import smf.database.DataStore;
import smf.events.AppEvent;
import smf.events.AppEventBus;
import smf.events.EventWatcher;
import smf.messages.MessageLog;
import smf.util.AppUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.ResultSet;

/**
 * SMF Full Application - DashboardPanel
 *
 * Home screen showing:
 *   - Live counts (products, customers, bills)
 *   - Low stock alerts
 *   - Real-time event log (every DB operation shown here)
 *   - Status bar with last event + timestamp
 *
 * Auto-refreshes on any DB event via EventWatcher.
 */
public class DashboardPanel extends JPanel implements EventWatcher {

    private final DataStore   db  = DataStore.getInstance();
    private final AppEventBus bus = AppEventBus.getInstance();
    private final MessageLog  log = MessageLog.getInstance();

    private JLabel productCount;
    private JLabel customerCount;
    private JLabel billCount;
    private JLabel lowStockCount;
    private JTextArea eventLog;
    private JTextArea lowStockArea;

    public DashboardPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        add(buildStatsRow(),   BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);

        bus.subscribe(this);
        refresh();
    }

    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 10, 0));

        productCount  = statCard("Products",  "0", new Color(41,  128, 185));
        customerCount = statCard("Customers", "0", new Color(39,  174, 96));
        billCount     = statCard("Bills",     "0", new Color(142, 68,  173));
        lowStockCount = statCard("Low Stock", "0", new Color(192, 57,  43));

        row.add(wrapCard(productCount,  "Products",  new Color(41,  128, 185)));
        row.add(wrapCard(customerCount, "Customers", new Color(39,  174, 96)));
        row.add(wrapCard(billCount,     "Bills",     new Color(142, 68,  173)));
        row.add(wrapCard(lowStockCount, "Low Stock", new Color(192, 57,  43)));
        return row;
    }

    private JLabel statCard(String title, String value, Color color) {
        JLabel label = new JLabel(value, SwingConstants.CENTER);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        label.setForeground(Color.WHITE);
        return label;
    }

    private JPanel wrapCard(JLabel valueLabel, String title, Color bg) {
        JPanel card = new JPanel(new BorderLayout(2, 2));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        titleLabel.setForeground(new Color(255, 255, 255, 180));

        card.add(valueLabel,  BorderLayout.CENTER);
        card.add(titleLabel,  BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));

        // Low stock list
        lowStockArea = new JTextArea();
        lowStockArea.setEditable(false);
        lowStockArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JPanel lowPanel = new JPanel(new BorderLayout());
        lowPanel.setBorder(new TitledBorder("⚠ Low Stock Alerts"));
        lowPanel.add(new JScrollPane(lowStockArea));

        // Event log
        eventLog = new JTextArea();
        eventLog.setEditable(false);
        eventLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        eventLog.setBackground(new Color(25, 30, 40));
        eventLog.setForeground(new Color(180, 220, 180));
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(new TitledBorder("Event Log"));

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> { log.clear(); eventLog.setText(""); });
        logPanel.add(new JScrollPane(eventLog), BorderLayout.CENTER);
        logPanel.add(clearBtn, BorderLayout.SOUTH);

        panel.add(lowPanel);
        panel.add(logPanel);
        return panel;
    }

    public void refresh() {
        // Counts
        productCount.setText(String.valueOf(count("products")));
        customerCount.setText(String.valueOf(count("customers")));
        billCount.setText(String.valueOf(count("bills")));

        // Low stock
        int low = 0;
        StringBuilder sb = new StringBuilder();
        ResultSet rs = db.Select("products", "name, current_stock, min_stock",
            "current_stock < min_stock", true);
        try {
            while (rs != null && rs.next()) {
                low++;
                sb.append(String.format("%-20s  stock: %d  min: %d%n",
                    rs.getString("name"),
                    rs.getInt("current_stock"),
                    rs.getInt("min_stock")));
            }
        } catch (Exception ignored) {}

        lowStockCount.setText(String.valueOf(low));
        lowStockArea.setText(low == 0 ? "All products in stock." : sb.toString());

        // Refresh event log
        eventLog.setText("");
        log.getAll().forEach(e -> eventLog.append(e + "\n"));
        eventLog.setCaretPosition(eventLog.getDocument().getLength());
    }

    private int count(String table) {
        try {
            ResultSet rs = db.Select(table, "COUNT(*) as n", null, false);
            if (rs != null && rs.next()) return rs.getInt("n");
        } catch (Exception ignored) {}
        return 0;
    }

    @Override
    public void update(Object event) {
        SwingUtilities.invokeLater(this::refresh);
    }

    public void unsubscribe() { bus.unsubscribe(this); }
}
