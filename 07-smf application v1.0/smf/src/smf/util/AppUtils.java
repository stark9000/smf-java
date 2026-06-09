package smf.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * SMF Library - AppUtils (copy from Tutorial 04)
 * Date/time utilities used by domain models.
 */
public class AppUtils {

    private AppUtils() {}

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT     = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String getDate()        { return LocalDate.now().format(DATE_FMT); }
    public static String getTime()        { return LocalDateTime.now().format(TIME_FMT); }
    public static String getDateAndTime() { return LocalDateTime.now().format(DATETIME_FMT); }

    public static String dateToString(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "";
    }

    public static LocalDate stringToDate(String s) {
        try { return LocalDate.parse(s, DATE_FMT); }
        catch (Exception e) { return null; }
    }
}
