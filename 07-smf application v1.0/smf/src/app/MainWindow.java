package app;

import app.panels.BillsPanel;
import app.panels.CustomersPanel;
import app.panels.DashboardPanel;
import app.panels.ProductsPanel;
import app.panels.SalesPanel;
import smf.events.AppEvent;
import smf.events.AppEventBus;
import smf.events.EventWatcher;
import smf.loader.AppConfig;
import smf.messages.MessageLog;
import smf.util.AppUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * SMF Full Application - MainWindow
 *
 * The main application window. Five tabbed panels, each independent, all wired
 * through AppEventBus.
 *
 * Tabs: Dashboard — stats, low stock alerts, live event log Sales — browse
 * products, build cart, checkout → bill Products — manage product catalog
 * Customers — manage customer records Bills — view saved invoices
 *
 * This is the completed version of what the original smf-lib NewJFrame +
 * shop/shade was building toward.
 *
 * Every panel: - Is an EventWatcher subscribed to AppEventBus - Refreshes
 * itself automatically when data changes - Uses UIFill for all Swing population
 * (Tutorial 02) - Uses domain models for all DB operations (Tutorial 05) -
 * Fires events through AppEventBus (Tutorial 03)
 *
 * None of the panels know about each other. They communicate only through the
 * event bus.
 */
public class MainWindow extends JFrame implements EventWatcher {

    private final AppEventBus bus = AppEventBus.getInstance();
    private final AppConfig config = AppConfig.getInstance();
    private final MessageLog log = MessageLog.getInstance();

    // Panels
    private DashboardPanel dashboard;
    private SalesPanel sales;
    private ProductsPanel products;
    private CustomersPanel customers;
    private BillsPanel bills;

    // Status bar
    private JLabel statusLabel;
    private JLabel timeLabel;

    public MainWindow() {
        super(buildTitle());

        buildUI();
        bus.subscribe(this);
        startClock();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        setSize(1100, 720);
        setLocationRelativeTo(null);
    }

    private static String buildTitle() {
        AppConfig config = AppConfig.getInstance();
        return config.get("app.title", "SMF Shop") + "  v" + config.get("app.version", "1.0");
    }

    private void buildUI() {
        setLayout(new BorderLayout(0, 0));

        // Build panels
        dashboard = new DashboardPanel();
        sales = new SalesPanel();
        products = new ProductsPanel();
        customers = new CustomersPanel();
        bills = new BillsPanel();

        // Tabbed pane
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("📊 Dashboard", dashboard);
        tabs.addTab("🛒 Sales", sales);
        tabs.addTab("📦 Products", products);
        tabs.addTab("👥 Customers", customers);
        tabs.addTab("🧾 Bills", bills);

        // Status bar
        statusLabel = new JLabel("  Ready");
        statusLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

        timeLabel = new JLabel(AppUtils.getDateAndTime() + "  ");
        timeLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                new EmptyBorder(2, 4, 2, 4)));
        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(timeLabel, BorderLayout.EAST);

        add(tabs, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    // -------------------------------------------------------------------------
    // EventWatcher — reacts to all bus events
    // This is the modernized NewJFrame.update() from the original smf-lib
    // -------------------------------------------------------------------------
    @Override
    public void update(Object event) {
        String evt = event.toString();
        SwingUtilities.invokeLater(() -> {
            switch (evt) {
                case AppEvent.DB_INSERT:
                    setStatus("Record saved", new Color(0, 120, 0));
                    break;
                case AppEvent.DB_UPDATE:
                    setStatus("Record updated", new Color(0, 80, 160));
                    break;
                case AppEvent.DB_DELETE:
                    setStatus("Record deleted", new Color(160, 60, 0));
                    break;
                case AppEvent.DB_ERROR:
                    setStatus("Database error — check MessageLog", Color.RED);
                    break;
                case AppEvent.APP_BUSY:
                    setStatus("Working...", new Color(160, 100, 0));
                    break;
                case AppEvent.APP_READY:
                    setStatus("Ready", new Color(0, 120, 0));
                    break;
                default:
                    setStatus(evt, Color.DARK_GRAY);
            }
        });
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText("  " + text);
        statusLabel.setForeground(color);
    }

    // -------------------------------------------------------------------------
    // Clock
    // -------------------------------------------------------------------------
    private void startClock() {
        Timer clock = new Timer(1000, e
                -> timeLabel.setText(AppUtils.getDateAndTime() + "  "));
        clock.start();
    }

    // -------------------------------------------------------------------------
    // Shutdown
    // -------------------------------------------------------------------------
    private void shutdown() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Exit application?", "Confirm Exit", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Unsubscribe all panels cleanly
        bus.unsubscribe(this);
        dashboard.unsubscribe();
        sales.unsubscribe();
        products.unsubscribe();
        customers.unsubscribe();
        bills.unsubscribe();

        log.info("[App] Shutdown at " + AppUtils.getDateAndTime());
        dispose();
        System.exit(0);
    }
}
