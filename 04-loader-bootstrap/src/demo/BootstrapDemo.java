package demo;

import smf.events.AppEvent;
import smf.events.AppEventBus;
import smf.events.EventWatcher;
import smf.loader.AppConfig;
import smf.loader.LoadResult;
import smf.loader.Loader;
import smf.messages.MessageLog;
import smf.util.AppUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * SMF Tutorial 04 - Loader & Application Bootstrap Demo
 *
 * Shows the complete startup sequence:
 *   1. SplashScreen shown immediately
 *   2. Loader runs on a background thread (keeps UI responsive)
 *   3. Progress bar and status label update in real time
 *   4. On success: splash closes, MainWindow opens
 *   5. On failure: error dialog shows boot results
 *
 * This is the pattern from the original smf-lib Loader.start():
 *   Load() → if isLoaded() → new NewJFrame().setVisible(true)
 *
 * Modernized with:
 *   - SwingWorker for background loading (no frozen UI)
 *   - Progress callbacks driving a real progress bar
 *   - Typed LoadResult list replacing raw HashSet errors
 *   - AppConfig loading settings from app.properties
 *   - EventWatcher on MainWindow reacting to DB events
 */
public class BootstrapDemo {

    public static void main(String[] args) {
        // Always start Swing apps on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            SplashScreen splash = new SplashScreen();
            splash.startLoading();
        });
    }

    // =========================================================================
    // SplashScreen
    // =========================================================================

    static class SplashScreen extends JWindow {

        private final JProgressBar progressBar;
        private final JLabel       statusLabel;
        private final JLabel       stepLabel;

        SplashScreen() {
            setSize(420, 220);
            setLocationRelativeTo(null);

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(new EmptyBorder(20, 24, 20, 24));
            panel.setBackground(new Color(30, 35, 45));

            // Title
            JLabel title = new JLabel("SMF Application", SwingConstants.CENTER);
            title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            title.setForeground(Color.WHITE);

            // Subtitle
            JLabel subtitle = new JLabel("Starting up...", SwingConstants.CENTER);
            subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            subtitle.setForeground(new Color(150, 160, 180));

            // Status label — shows current step name
            stepLabel = new JLabel("Initializing...", SwingConstants.CENTER);
            stepLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            stepLabel.setForeground(new Color(100, 200, 100));

            // Progress bar
            progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            progressBar.setString("0%");
            progressBar.setForeground(new Color(80, 160, 255));
            progressBar.setBackground(new Color(50, 55, 65));

            // Status label — shows OK / FAILED
            statusLabel = new JLabel(" ", SwingConstants.CENTER);
            statusLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            statusLabel.setForeground(new Color(180, 180, 180));

            JPanel center = new JPanel(new GridLayout(3, 1, 4, 4));
            center.setOpaque(false);
            center.add(stepLabel);
            center.add(progressBar);
            center.add(statusLabel);

            panel.add(title,    BorderLayout.NORTH);
            panel.add(center,   BorderLayout.CENTER);
            panel.add(subtitle, BorderLayout.SOUTH);

            setContentPane(panel);
            setVisible(true);
        }

        /**
         * Run the Loader on a background thread.
         * UI updates always happen on the EDT via SwingUtilities.invokeLater.
         */
        void startLoading() {
            Loader loader = Loader.getInstance();

            // Progress callback — updates the bar
            loader.onProgress(pct -> SwingUtilities.invokeLater(() -> {
                progressBar.setValue(pct);
                progressBar.setString(pct + "%");
            }));

            // Step callback — updates labels after each service loads
            loader.onStep(result -> SwingUtilities.invokeLater(() -> {
                stepLabel.setText("Loading: " + result.getServiceName());
                statusLabel.setText(result.toString());
                if (result.isFailed()) {
                    stepLabel.setForeground(new Color(255, 80, 80));
                }
            }));

            // Run loader on background thread — keeps splash screen responsive
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return loader.load();
                }

                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        if (success) {
                            onLoadSuccess();
                        } else {
                            onLoadFailure(loader);
                        }
                    } catch (Exception e) {
                        showError("Unexpected error during boot: " + e.getMessage());
                    }
                }
            };

            // Small delay so the splash is visible before loading starts
            Timer startDelay = new Timer(400, e -> worker.execute());
            startDelay.setRepeats(false);
            startDelay.start();
        }

        private void onLoadSuccess() {
            dispose(); // close splash
            new MainWindow().setVisible(true);
        }

        private void onLoadFailure(Loader loader) {
            // Build a readable error report
            StringBuilder sb = new StringBuilder();
            sb.append("Bootstrap failed. Results:\n\n");
            loader.getResults().forEach(r -> sb.append(r).append("\n"));

            showError(sb.toString());
        }

        private void showError(String message) {
            JTextArea area = new JTextArea(message);
            area.setEditable(false);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new Dimension(500, 300));

            JOptionPane.showMessageDialog(this,
                scroll,
                "Startup Failed",
                JOptionPane.ERROR_MESSAGE);

            System.exit(1);
        }
    }

    // =========================================================================
    // MainWindow
    // Implements EventWatcher — reacts to events from the AppEventBus.
    // This mirrors how NewJFrame implemented EventWatcher in the original smf-lib.
    // =========================================================================

    static class MainWindow extends JFrame implements EventWatcher {

        private final AppEventBus bus = AppEventBus.getInstance();
        private final MessageLog  log = MessageLog.getInstance();
        private final AppConfig   config = AppConfig.getInstance();

        private JLabel  statusBar;
        private JTextArea logArea;

        MainWindow() {
            super(config.get("app.title", "SMF Application")
                + " v" + config.get("app.version", "1.0"));

            buildUI();

            // Register with event bus — this is the modernized RegisterWatcher()
            bus.subscribe(this);

            // Show boot log on startup
            refreshLog();

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(700, 500);
            setLocationRelativeTo(null);

            // Unsubscribe cleanly on close
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    bus.unsubscribe(MainWindow.this);
                }
            });
        }

        private void buildUI() {
            setLayout(new BorderLayout(6, 6));
            ((JPanel) getContentPane())
                .setBorder(new EmptyBorder(8, 8, 4, 8));

            // --- Top: config info panel ---
            JPanel infoPanel = new JPanel(new GridLayout(2, 4, 8, 4));
            infoPanel.setBorder(BorderFactory.createTitledBorder("Loaded Configuration"));

            infoPanel.add(new JLabel("DB Host:"));
            infoPanel.add(bold(config.get("db.host", "?")));
            infoPanel.add(new JLabel("DB Name:"));
            infoPanel.add(bold(config.get("db.name", "?")));
            infoPanel.add(new JLabel("DB Port:"));
            infoPanel.add(bold(config.get("db.port", "?")));
            infoPanel.add(new JLabel("App Version:"));
            infoPanel.add(bold(config.get("app.version", "?")));

            // --- Center: action buttons + log ---
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            buttonPanel.setBorder(BorderFactory.createTitledBorder("Simulate Events"));

            String[] events = {
                AppEvent.DB_INSERT, AppEvent.DB_SELECT,
                AppEvent.DB_UPDATE, AppEvent.DB_DELETE,
                AppEvent.DB_ERROR,  AppEvent.DATA_CHANGED
            };
            for (String event : events) {
                JButton btn = new JButton(event.replace("--", ""));
                btn.addActionListener(e -> {
                    // Simulate a button busy state during the "operation"
                    Object[] state = AppUtils.buttonBusy(btn);
                    log.info("Simulating: " + event);
                    bus.publish(event);
                    // Restore after 800ms
                    Timer t = new Timer(800, x -> AppUtils.buttonRestore(btn, state));
                    t.setRepeats(false);
                    t.start();
                });
                buttonPanel.add(btn);
            }

            // --- Log area ---
            logArea = new JTextArea();
            logArea.setEditable(false);
            logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            JScrollPane logScroll = new JScrollPane(logArea);
            logScroll.setBorder(BorderFactory.createTitledBorder("MessageLog"));

            // --- Bottom: status bar ---
            statusBar = new JLabel("Ready  |  " + AppUtils.getDateAndTime());
            statusBar.setBorder(new EmptyBorder(4, 4, 2, 4));
            statusBar.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

            JPanel centerPanel = new JPanel(new BorderLayout(4, 4));
            centerPanel.add(buttonPanel, BorderLayout.NORTH);
            centerPanel.add(logScroll,   BorderLayout.CENTER);

            add(infoPanel,   BorderLayout.NORTH);
            add(centerPanel, BorderLayout.CENTER);
            add(statusBar,   BorderLayout.SOUTH);
        }

        /**
         * EventWatcher.update() — called by AppEventBus on every event.
         * This is the modernized version of NewJFrame.update() from smf-lib.
         */
        @Override
        public void update(Object event) {
            SwingUtilities.invokeLater(() -> {
                String evt = event.toString();

                // Update status bar
                statusBar.setText(evt + "  |  " + AppUtils.getDateAndTime());

                // Color-code status
                switch (evt) {
                    case AppEvent.DB_ERROR:
                        statusBar.setForeground(Color.RED);
                        break;
                    case AppEvent.APP_BUSY:
                        statusBar.setForeground(new Color(200, 120, 0));
                        break;
                    case AppEvent.APP_READY:
                        statusBar.setForeground(new Color(0, 140, 0));
                        break;
                    default:
                        statusBar.setForeground(new Color(0, 100, 180));
                }

                // Refresh the log view
                refreshLog();
            });
        }

        private void refreshLog() {
            logArea.setText("");
            log.getAll().forEach(entry -> logArea.append(entry + "\n"));
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }

        private static JLabel bold(String text) {
            JLabel label = new JLabel(text);
            label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            return label;
        }
    }
}
