package smf.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * SMF Library - AppUtils
 *
 * General-purpose utility methods used across the application.
 *
 * The original smf-lib had:
 *   - Extra interface (getDate, getTime, getDateAndTime, gregorian_to_string, etc.)
 *   - Extrax implementation (most methods threw UnsupportedOperationException)
 *   - TaskButtonx (animated busy-state button)
 *
 * AppUtils completes the unfinished date/time and image methods
 * using Java 8's new date/time API (LocalDate, LocalDateTime)
 * instead of the old GregorianCalendar approach.
 *
 * Original concept by: stark (Extra / Extrax)
 * Modernized for Java 8 — GregorianCalendar replaced with LocalDate/LocalDateTime
 */
public class AppUtils {

    // Prevent instantiation — static utility class
    private AppUtils() {}

    // -------------------------------------------------------------------------
    // Date and Time
    // -------------------------------------------------------------------------

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT     = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Get today's date as a formatted string.
     * Format: yyyy-MM-dd
     *
     * Example: "2013-07-05"
     */
    public static String getDate() {
        return LocalDate.now().format(DATE_FMT);
    }

    /**
     * Get the current time as a formatted string.
     * Format: HH:mm:ss
     *
     * Example: "14:23:01"
     */
    public static String getTime() {
        return LocalDateTime.now().format(TIME_FMT);
    }

    /**
     * Get the current date and time as a formatted string.
     * Format: yyyy-MM-dd HH:mm:ss
     *
     * Example: "2013-07-05 14:23:01"
     */
    public static String getDateAndTime() {
        return LocalDateTime.now().format(DATETIME_FMT);
    }

    /**
     * Convert a LocalDate to a "yyyy-MM-dd" string.
     * Replaces the original gregorian_to_string(GregorianCalendar).
     *
     * @param date The LocalDate to format.
     * @return Formatted date string.
     */
    public static String dateToString(LocalDate date) {
        return date.format(DATE_FMT);
    }

    /**
     * Parse a "yyyy-MM-dd" string into a LocalDate.
     * Replaces the original string_to_gregorian(String).
     *
     * @param dateString Date string in yyyy-MM-dd format.
     * @return Parsed LocalDate, or null if parsing fails.
     */
    public static LocalDate stringToDate(String dateString) {
        try {
            return LocalDate.parse(dateString, DATE_FMT);
        } catch (Exception e) {
            System.err.println("[AppUtils] Invalid date string: " + dateString);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Image loading
    // -------------------------------------------------------------------------

    /**
     * Load an image from the classpath (bundled inside the JAR).
     *
     * Example:
     *   BufferedImage icon = AppUtils.loadImage("/images/logo.png");
     *   if (icon != null) label.setIcon(new ImageIcon(icon));
     *
     * @param resourcePath Classpath-relative path, starting with /
     * @return BufferedImage or null if not found.
     */
    public static BufferedImage loadImage(String resourcePath) {
        URL url = AppUtils.class.getResource(resourcePath);
        if (url == null) {
            System.err.println("[AppUtils] Image not found: " + resourcePath);
            return null;
        }
        try {
            return ImageIO.read(url);
        } catch (Exception e) {
            System.err.println("[AppUtils] Failed to load image: " + e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // TaskButton — busy state for JButton
    // -------------------------------------------------------------------------

    /**
     * Put a JButton into a "busy" state — disable it and show a busy indicator.
     * Preserves the original state so it can be restored later.
     *
     * Original concept: TaskButtonx.install() / Animate()
     *
     * Usage:
     *   Object[] state = AppUtils.buttonBusy(myButton);
     *   // ... do work ...
     *   AppUtils.buttonRestore(myButton, state);
     *
     * @param button The JButton to put into busy state.
     * @return Saved state array — pass to buttonRestore() when done.
     */
    public static Object[] buttonBusy(javax.swing.JButton button) {
        if (button == null) return null;

        // Save original state
        Object[] state = new Object[]{
            button.getText(),
            button.isEnabled(),
            button.getIcon()
        };

        // Apply busy state on EDT
        javax.swing.SwingUtilities.invokeLater(() -> {
            button.setText("Working...");
            button.setEnabled(false);
        });

        return state;
    }

    /**
     * Restore a JButton from busy state.
     *
     * @param button The JButton to restore.
     * @param state  The state array returned by buttonBusy().
     */
    public static void buttonRestore(javax.swing.JButton button, Object[] state) {
        if (button == null || state == null) return;

        javax.swing.SwingUtilities.invokeLater(() -> {
            button.setText((String) state[0]);
            button.setEnabled((Boolean) state[1]);
            button.setIcon((javax.swing.Icon) state[2]);
        });
    }
}
