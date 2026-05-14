import smf.database.DataStore;
import smf.messages.SystemMessages;

import java.sql.ResultSet;

/**
 * SMF Tutorial 01 - Database Connection Example
 *
 * Shows how to use DataStore to connect to MySQL,
 * perform CRUD operations, listen to events, and read system messages.
 *
 * Requirements:
 *   - MySQL running locally (or change the IP/port)
 *   - mysql-connector-java.jar in your classpath
 *   - A database and table matching the examples below
 *
 * To create the test table:
 *   CREATE DATABASE smf_test;
 *   USE smf_test;
 *   CREATE TABLE products (
 *       id    INT AUTO_INCREMENT PRIMARY KEY,
 *       name  VARCHAR(100),
 *       price DECIMAL(10,2)
 *   );
 */
public class Example {

    public static void main(String[] args) {

        // --- 1. Get the DataStore singleton ---
        DataStore db = DataStore.getInstance();

        // --- 2. Configure connection ---
        db.setServerIP("127.0.0.1");
        db.setServerPort("3306");
        db.setDatabaseName("smf_test");
        db.setDatabaseUser("root");
        db.setDatabasePassword("yourpassword");

        // --- 3. Register an event listener (Java 8 lambda) ---
        // This fires on every DB operation
        db.registerWatcher(event -> System.out.println("[Event] " + event));

        // --- 4. INSERT ---
        System.out.println("\n-- INSERT --");
        boolean inserted = db.Insert(
            "INSERT INTO products (name, price)",
            new String[]{"Arduino Nano", "4.50"}
        );
        System.out.println("Insert result: " + inserted);

        // --- 5. SELECT all ---
        System.out.println("\n-- SELECT ALL --");
        ResultSet rs = db.Select("products", "*", null, false);
        try {
            while (rs != null && rs.next()) {
                System.out.println(
                    "ID: "    + rs.getInt("id") +
                    " Name: " + rs.getString("name") +
                    " Price: " + rs.getDouble("price")
                );
            }
        } catch (Exception e) {
            System.out.println("Error reading results: " + e.getMessage());
        }

        // --- 6. SELECT with WHERE ---
        System.out.println("\n-- SELECT WHERE --");
        ResultSet rs2 = db.Select("products", "name, price", "price < 10.00", true);
        try {
            while (rs2 != null && rs2.next()) {
                System.out.println(rs2.getString("name") + " - $" + rs2.getDouble("price"));
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        // --- 7. UPDATE ---
        System.out.println("\n-- UPDATE --");
        boolean updated = db.Update("products", "price = 5.00", "name = 'Arduino Nano'");
        System.out.println("Update result: " + updated);

        // --- 8. DELETE ---
        System.out.println("\n-- DELETE --");
        boolean deleted = db.Delete("products", "name", "Arduino Nano");
        System.out.println("Delete result: " + deleted);

        // --- 9. Check system messages (errors, status logs) ---
        System.out.println("\n-- SYSTEM MESSAGES --");
        SystemMessages.getInstance().getMessages().forEach(System.out::println);

        // --- 10. Close connection on exit ---
        db.closeConnection();
    }
}
