package demo;

import smf.events.AppEvent;
import smf.events.AppEventBus;
import smf.events.EventWatcher;
import smf.messages.MessageLog;
import smf.messages.MessageLog.Level;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * SMF Tutorial 03 - EventWatcher & MessageCenter Demo
 *
 * Shows the Observer pattern in practice — three independent panels
 * that know nothing about each other, all wired together through
 * the AppEventBus without any direct references between them.
 *
 * Panels in this demo:
 *   1. ControlPanel   — publishes events (simulates DB operations)
 *   2. StatusPanel    — subscribes and shows current status (implements EventWatcher)
 *   3. LogPanel       — subscribes via lambda, shows the MessageLog
 *
 * This mirrors how smf-lib was used:
 *   - DataStore published "--insert", "--select" etc.
 *   - NewJFrame implemented EventWatcher and reacted in update()
 *
 * Here we go further: multiple publishers, multiple subscribers,
 * typed event constants, and a proper message log with levels.
 */
public class EventWatcherDemo extends JFrame {

    private final AppEventBus bus = AppEventBus.getInstance();
    private final MessageLog  log = MessageLog.getInstance();

    public EventWatcherDemo() {
        super("SMF Tutorial 03 - EventWatcher & MessageCenter");

        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Build the three panels
        ControlPanel controlPanel = new ControlPanel(bus, log);
        StatusPanel  statusPanel  = new StatusPanel(bus);
        LogPanel     logPanel     = new LogPanel(bus, log);

        // Layout: controls top-left, status top-right, log at bottom
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        topPanel.add(controlPanel);
        topPanel.add(statusPanel);

        add(topPanel,  BorderLayout.CENTER);
        add(logPanel,  BorderLayout.SOUTH);

        // Clean up subscriptions when window closes
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                statusPanel.unsubscribe();
                logPanel.unsubscribe();
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(820, 580);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // =========================================================================
    // Panel 1 — ControlPanel
    // Publishes events. Simulates DB operations and app lifecycle events.
    // =========================================================================

    static class ControlPanel extends JPanel {

        ControlPanel(AppEventBus bus, MessageLog log) {
            setLayout(new GridLayout(0, 1, 4, 4));
            setBorder(titledBorder("1. Control Panel — publishes events"));

            addButton("Simulate INSERT",
                "Product saved to database",
                AppEvent.DB_INSERT, bus, log, Level.INFO);

            addButton("Simulate SELECT",
                "Products loaded from database",
                AppEvent.DB_SELECT, bus, log, Level.INFO);

            addButton("Simulate UPDATE",
                "Product record updated",
                AppEvent.DB_UPDATE, bus, log, Level.INFO);

            addButton("Simulate DELETE",
                "Product record deleted",
                AppEvent.DB_DELETE, bus, log, Level.INFO);

            addButton("Simulate DB ERROR",
                "Connection refused: timeout after 5000ms",
                AppEvent.DB_ERROR, bus, log, Level.ERROR);

            addButton("App BUSY",
                "Background task started",
                AppEvent.APP_BUSY, bus, log, Level.INFO);

            addButton("App READY",
                "Background task completed",
                AppEvent.APP_READY, bus, log, Level.INFO);

            addButton("User LOGIN",
                "User authenticated: admin",
                AppEvent.USER_LOGIN, bus, log, Level.INFO);

            addButton("Data CHANGED",
                "Product list was modified",
                AppEvent.DATA_CHANGED, bus, log, Level.WARN);
        }

        private void addButton(String label, String logMsg,
                               String event, AppEventBus bus,
                               MessageLog log, Level level) {
            JButton btn = new JButton(label);
            btn.addActionListener(e -> {
                // 1. Log the message
                switch (level) {
                    case INFO:  log.info(logMsg);  break;
                    case WARN:  log.warn(logMsg);  break;
                    case ERROR: log.error(logMsg); break;
                }
                // 2. Publish event to all subscribers
                bus.publish(event);
            });
            add(btn);
        }
    }

    // =========================================================================
    // Panel 2 — StatusPanel
    // Subscribes by implementing EventWatcher directly (classic smf-lib style).
    // Reacts to events and updates its UI.
    // =========================================================================

    static class StatusPanel extends JPanel implements EventWatcher {

        private final AppEventBus bus;
        private final JLabel  statusLabel;
        private final JLabel  lastEventLabel;
        private final JLabel  subscriberLabel;
        private final JProgressBar busyBar;
        private       int     eventCount = 0;

        StatusPanel(AppEventBus bus) {
            this.bus = bus;
            setLayout(new GridLayout(0, 1, 4, 6));
            setBorder(titledBorder("2. Status Panel — implements EventWatcher"));

            statusLabel      = makeLabel("Status: IDLE", Color.DARK_GRAY, 14, true);
            lastEventLabel   = makeLabel("Last event: (none)", Color.GRAY, 12, false);
            subscriberLabel  = makeLabel("Subscribers: 0", Color.GRAY, 12, false);
            busyBar          = new JProgressBar();
            busyBar.setIndeterminate(false);
            busyBar.setString("Idle");
            busyBar.setStringPainted(true);

            add(statusLabel);
            add(lastEventLabel);
            add(subscriberLabel);
            add(new JLabel("Activity:"));
            add(busyBar);

            // Register with the bus — this panel now receives all events
            bus.subscribe(this);
            refreshSubscriberCount();
        }

        /**
         * This is the original smf-lib pattern:
         * NewJFrame implemented EventWatcher and handled events here.
         *
         * Extended here to handle multiple event types with typed constants.
         */
        @Override
        public void update(Object event) {
            eventCount++;
            String evt = event.toString();

            // Always update on EDT when changing Swing components
            SwingUtilities.invokeLater(() -> {
                lastEventLabel.setText("Last event: " + evt
                    + "  (#" + eventCount + ")");
                refreshSubscriberCount();

                // React differently based on event type
                switch (evt) {
                    case AppEvent.DB_INSERT:
                    case AppEvent.DB_UPDATE:
                    case AppEvent.DB_DELETE:
                        setStatus("DB WRITE OK", new Color(0, 140, 0));
                        break;

                    case AppEvent.DB_SELECT:
                        setStatus("DB READ OK", new Color(0, 100, 200));
                        break;

                    case AppEvent.DB_ERROR:
                        setStatus("DB ERROR", Color.RED);
                        break;

                    case AppEvent.APP_BUSY:
                        setStatus("BUSY...", new Color(200, 120, 0));
                        busyBar.setIndeterminate(true);
                        busyBar.setString("Working...");
                        break;

                    case AppEvent.APP_READY:
                        setStatus("READY", new Color(0, 140, 0));
                        busyBar.setIndeterminate(false);
                        busyBar.setString("Done");
                        break;

                    case AppEvent.USER_LOGIN:
                        setStatus("USER LOGGED IN", new Color(80, 0, 180));
                        break;

                    case AppEvent.USER_LOGOUT:
                        setStatus("LOGGED OUT", Color.DARK_GRAY);
                        break;

                    case AppEvent.DATA_CHANGED:
                        setStatus("DATA CHANGED", new Color(180, 100, 0));
                        break;

                    default:
                        setStatus("EVENT: " + evt, Color.DARK_GRAY);
                }
            });
        }

        /** Call this when the panel is closed to avoid memory leaks. */
        void unsubscribe() {
            bus.unsubscribe(this);
        }

        private void setStatus(String text, Color color) {
            statusLabel.setText("Status: " + text);
            statusLabel.setForeground(color);
        }

        private void refreshSubscriberCount() {
            subscriberLabel.setText("Bus subscribers: " + bus.subscriberCount());
        }

        private static JLabel makeLabel(String text, Color color, int size, boolean bold) {
            JLabel label = new JLabel(text);
            label.setForeground(color);
            label.setFont(new Font(Font.SANS_SERIF,
                bold ? Font.BOLD : Font.PLAIN, size));
            return label;
        }
    }

    // =========================================================================
    // Panel 3 — LogPanel
    // Subscribes using a Java 8 lambda — no need to implement EventWatcher.
    // Displays the MessageLog with color-coded levels.
    // =========================================================================

    static class LogPanel extends JPanel {

        private final AppEventBus bus;
        private final MessageLog  log;
        private final JTextArea   logArea;
        private final JLabel      errorBadge;
        private final EventWatcher subscription; // keep reference for unsubscribe

        LogPanel(AppEventBus bus, MessageLog log) {
            this.bus = bus;
            this.log = log;

            setLayout(new BorderLayout(4, 4));
            setBorder(titledBorder("3. Log Panel — subscribes via lambda"));
            setPreferredSize(new Dimension(800, 180));

            // Log text area
            logArea = new JTextArea();
            logArea.setEditable(false);
            logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            logArea.setBackground(new Color(30, 30, 30));
            logArea.setForeground(new Color(200, 200, 200));

            // Error badge
            errorBadge = new JLabel("  No errors  ");
            errorBadge.setOpaque(true);
            errorBadge.setBackground(new Color(0, 140, 0));
            errorBadge.setForeground(Color.WHITE);
            errorBadge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

            // Controls
            JButton clearBtn = new JButton("Clear Log");
            clearBtn.addActionListener(e -> {
                log.clear();
                logArea.setText("");
                errorBadge.setText("  No errors  ");
                errorBadge.setBackground(new Color(0, 140, 0));
            });

            JButton errorsBtn = new JButton("Show Errors Only");
            errorsBtn.addActionListener(e -> {
                logArea.setText("");
                log.getByLevel(Level.ERROR)
                   .forEach(entry -> logArea.append(entry + "\n"));
            });

            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            controls.add(errorBadge);
            controls.add(clearBtn);
            controls.add(errorsBtn);

            add(new JScrollPane(logArea), BorderLayout.CENTER);
            add(controls, BorderLayout.SOUTH);

            // Subscribe using a Java 8 lambda — no interface implementation needed
            // Keep the reference so we can unsubscribe when the panel is closed
            subscription = bus.subscribe(event -> {
                SwingUtilities.invokeLater(() -> refreshLog());
            });
        }

        private void refreshLog() {
            logArea.setText("");
            log.getAll().forEach(entry -> {
                logArea.append(entry + "\n");
            });
            logArea.setCaretPosition(logArea.getDocument().getLength());

            // Update error badge
            if (log.hasErrors()) {
                long errorCount = log.getByLevel(Level.ERROR).size();
                errorBadge.setText("  " + errorCount + " error(s)  ");
                errorBadge.setBackground(new Color(180, 0, 0));
            }
        }

        void unsubscribe() {
            bus.unsubscribe(subscription);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    static TitledBorder titledBorder(String title) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), title);
    }

    // =========================================================================
    // Main
    // =========================================================================

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(EventWatcherDemo::new);
    }
}
