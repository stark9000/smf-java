package app;

import smf.loader.Loader;
import smf.messages.MessageLog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * SMF Full Application - Main Entry Point
 *
 * Brings together everything from Tutorials 01–05:
 *
 * Tutorial 01 — DataStore, DatabaseConnection, DatabaseFunctions Tutorial 02 —
 * UIFill (JTable, JComboBox, JList from ResultSet) Tutorial 03 — AppEventBus,
 * EventWatcher, MessageLog Tutorial 04 — Loader, AppConfig, SplashScreen,
 * SwingWorker Tutorial 05 — CustomerModel, ProductModel, BillModel,
 * AbstractPerson
 *
 * Boot sequence (Tutorial 04 pattern): 1. Show SplashScreen 2. Run Loader on
 * background thread 3. Loader initializes: MessageLog → AppEventBus → AppConfig
 * → DataStore 4. If OK → show MainWindow 5. If FAIL → show error dialog (DB not
 * connected = warning only, app still runs)
 */
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ignored) {
        }

        SwingUtilities.invokeLater(() -> new SplashScreen().startLoading());
    }

    // =========================================================================
    // SplashScreen
    // =========================================================================
    static class SplashScreen extends JWindow {

        private final JProgressBar progressBar;
        private final JLabel stepLabel;
        private final JLabel statusLabel;

        SplashScreen() {
            setSize(440, 200);
            setLocationRelativeTo(null);

            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setBackground(new Color(28, 35, 50));
            panel.setBorder(new EmptyBorder(20, 24, 16, 24));

            JLabel title = new JLabel("SMF Shop Application", SwingConstants.CENTER);
            title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            title.setForeground(Color.WHITE);

            stepLabel = new JLabel("Initializing...", SwingConstants.CENTER);
            stepLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            stepLabel.setForeground(new Color(100, 200, 100));

            progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            progressBar.setString("0%");
            progressBar.setForeground(new Color(80, 160, 255));
            progressBar.setBackground(new Color(50, 60, 80));

            statusLabel = new JLabel(" ", SwingConstants.CENTER);
            statusLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            statusLabel.setForeground(new Color(140, 140, 140));

            JPanel center = new JPanel(new GridLayout(3, 1, 4, 4));
            center.setOpaque(false);
            center.add(stepLabel);
            center.add(progressBar);
            center.add(statusLabel);

            panel.add(title, BorderLayout.NORTH);
            panel.add(center, BorderLayout.CENTER);
            setContentPane(panel);
            setVisible(true);
        }

        void startLoading() {
            Loader loader = Loader.getInstance();

            loader.onProgress(pct -> SwingUtilities.invokeLater(() -> {
                progressBar.setValue(pct);
                progressBar.setString(pct + "%");
            }));

            loader.onStep(result -> SwingUtilities.invokeLater(() -> {
                stepLabel.setText("Loading: " + result.getServiceName());
                statusLabel.setText(result.toString());
                if (result.isFailed()) {
                    stepLabel.setForeground(new Color(255, 100, 100));
                }
            }));

            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return loader.load();
                }

                @Override
                protected void done() {
                    try {
                        boolean ok = get();
                        // Even if DB unreachable, we still launch —
                        // the app works in demo mode without a DB connection
                        dispose();
                        MainWindow win = new MainWindow();
                        win.setVisible(true);

                        if (!ok) {
                            MessageLog.getInstance().warn(
                                    "[Main] Some services failed — running in limited mode");
                        }
                    } catch (Exception e) {
                        showError("Boot failed: " + e.getMessage());
                    }
                }
            };

            // Small delay so splash is visible
            Timer t = new Timer(400, e -> worker.execute());
            t.setRepeats(false);
            t.start();
        }

        private void showError(String msg) {
            JOptionPane.showMessageDialog(this, msg, "Startup Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
