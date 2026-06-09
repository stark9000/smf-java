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
import java.sql.SQLException;

/**
 * SMF Shop - DashboardPanel
 *
 * Fixes vs tutorial version:
 *   - All DB calls use try-with-resources on CloseableResult
 *   - No ResultSet or Statement leaks
 */
public class DashboardPanel extends JPanel implements EventWatcher {

    private final DataStore   db  = DataStore.getInstance();
    private final AppEventBus bus = AppEventBus.getInstance();
    private final MessageLog  log = MessageLog.getInstance();

    private JLabel    productCount, customerCount, billCount, lowStockCount;
    private JTextArea lowStockArea, eventLogArea;

    public DashboardPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        add(buildStatsRow(),    BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        bus.subscribe(this);
        refresh();
    }

    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 10, 0));
        row.add(statCard("Products",  productCount  = bigLabel("0"), new Color(41,  128, 185)));
        row.add(statCard("Customers", customerCount = bigLabel("0"), new Color(39,  174, 96)));
        row.add(statCard("Bills",     billCount     = bigLabel("0"), new Color(142, 68,  173)));
        row.add(statCard("Low Stock", lowStockCount = bigLabel("0"), new Color(192, 57,  43)));
        return row;
    }

    private JPanel statCard(String title, JLabel value, Color bg) {
        JPanel card = new JPanel(new BorderLayout(2, 2));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setForeground(new Color(255, 255, 255, 180));
        t.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        card.add(value, BorderLayout.CENTER);
        card.add(t,     BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));

        lowStockArea = new JTextArea();
        lowStockArea.setEditable(false);
        lowStockArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JPanel lowPanel = new JPanel(new BorderLayout());
        lowPanel.setBorder(new TitledBorder("⚠ Low Stock Alerts"));
        lowPanel.add(new JScrollPane(lowStockArea));

        eventLogArea = new JTextArea();
        eventLogArea.setEditable(false);
        eventLogArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        eventLogArea.setBackground(new Color(25, 30, 40));
        eventLogArea.setForeground(new Color(180, 220, 180));

        JButton clearBtn = new JButton("Clear Log");
        clearBtn.addActionListener(e -> { log.clear(); eventLogArea.setText(""); });

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(new TitledBorder("Event Log"));
        logPanel.add(new JScrollPane(eventLogArea), BorderLayout.CENTER);
        logPanel.add(clearBtn, BorderLayout.SOUTH);

        panel.add(lowPanel);
        panel.add(logPanel);
        return panel;
    }

    public void refresh() {
        // Counts — CloseableResult in try-with-resources, no leak
        productCount.setText(String.valueOf(db.count("products")));
        customerCount.setText(String.valueOf(db.count("customers")));
        billCount.setText(String.valueOf(db.count("bills")));

        // Low stock — try-with-resources
        int low = 0;
        StringBuilder sb = new StringBuilder();
        try (DataStore.CloseableResult cr = db.query(
                "SELECT name, current_stock, min_stock FROM products " +
                "WHERE current_stock < min_stock ORDER BY name")) {
            ResultSet rs = cr.getResultSet();
            if (rs != null) {
                while (rs.next()) {
                    low++;
                    sb.append(String.format("%-20s  stock: %d  min: %d%n",
                        rs.getString("name"),
                        rs.getInt("current_stock"),
                        rs.getInt("min_stock")));
                }
            }
        } catch (SQLException e) {
            log.error("[Dashboard] Low stock query failed: " + e.getMessage());
        }

        lowStockCount.setText(String.valueOf(low));
        lowStockArea.setText(low == 0 ? "All products in stock." : sb.toString());

        // Refresh event log
        eventLogArea.setText("");
        log.getAll().forEach(e -> eventLogArea.append(e + "\n"));
        eventLogArea.setCaretPosition(eventLogArea.getDocument().getLength());
    }

    @Override
    public void update(Object event) {
        SwingUtilities.invokeLater(this::refresh);
    }

    public void unsubscribe() { bus.unsubscribe(this); }

    private static JLabel bigLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        l.setForeground(Color.WHITE);
        return l;
    }
}
