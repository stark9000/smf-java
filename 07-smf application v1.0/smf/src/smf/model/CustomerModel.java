package smf.model;

import smf.database.DataStore;
import smf.messages.MessageLog;
import smf.model.structures.Customer;
import smf.util.AppUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SMF - CustomerModel
 *
 * Fixes vs tutorial version:
 *   - All SQL uses PreparedStatements via DataStore.insert/update/delete
 *   - findById, findByName use queryPrepared — no string concatenation
 *   - findAll returns CloseableResult — caller manages lifecycle
 *   - findByName uses LIKE with ? parameter
 */
public class CustomerModel extends AbstractPerson implements Customer {

    private final DataStore  db  = DataStore.getInstance();
    private final MessageLog log = MessageLog.getInstance();

    private int     customerId   = -1;
    private String  dateJoined   = AppUtils.getDate();
    private boolean active       = true;
    private String  customerNote = "";

    private String   creditCardNumber = "";
    private String   creditCardType   = "";
    private boolean  creditCardActive = false;
    private String   pin              = "";
    private double   holdings         = 0.0;
    private double[] latePayments     = {};

    public CustomerModel() {}

    // -------------------------------------------------------------------------
    // Customer interface
    // -------------------------------------------------------------------------

    @Override public void    setCustomerId(int id)          { this.customerId = id; }
    @Override public int     getCustomerId()                { return customerId; }
    @Override public void    setDateJoined(String d)        { this.dateJoined = d; }
    @Override public String  getDateJoined()                { return dateJoined; }
    @Override public void    setActive(boolean a)           { this.active = a; }
    @Override public boolean isActive()                     { return active; }
    @Override public void    setCustomerNote(String n)      { this.customerNote = n; }
    @Override public String  getCustomerNote()              { return customerNote; }
    @Override public void    setCreditCardNumber(String n)  { this.creditCardNumber = n; }
    @Override public String  getCreditCardNumber()          { return creditCardNumber; }
    @Override public void    setCreditCardType(String t)    { this.creditCardType = t; }
    @Override public String  getCreditCardType()            { return creditCardType; }
    @Override public void    setCreditCardActive(boolean a) { this.creditCardActive = a; }
    @Override public boolean isCreditCardActive()           { return creditCardActive; }
    @Override public void    setPin(String p)               { this.pin = p; }
    @Override public String  getPin()                       { return pin; }
    @Override public void    setHoldings(double h)          { this.holdings = h; }
    @Override public double  getHoldings()                  { return holdings; }
    @Override public void    setLatePayments(double[] p)    { this.latePayments = p; }
    @Override public double[] getLatePayments()             { return latePayments; }

    // -------------------------------------------------------------------------
    // Database — all PreparedStatements, no concatenation
    // -------------------------------------------------------------------------

    public boolean save() {
        return customerId == -1 ? insert() : update();
    }

    private boolean insert() {
        return db.insert(
            "INSERT INTO customers (name, full_name, email, phone, city, country, date_joined, active, note) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            getName(), getFullName(), getEmail(),
            getHandPhoneNumber(), getCity(), getCountry(),
            dateJoined, active ? 1 : 0, customerNote
        );
    }

    private boolean update() {
        return db.update(
            "UPDATE customers SET name=?, full_name=?, email=?, phone=?, city=?, country=?, active=?, note=? " +
            "WHERE id=?",
            getName(), getFullName(), getEmail(),
            getHandPhoneNumber(), getCity(), getCountry(),
            active ? 1 : 0, customerNote,
            customerId
        );
    }

    public boolean delete() {
        if (customerId == -1) return false;
        return db.delete("DELETE FROM customers WHERE id=?", customerId);
    }

    // -------------------------------------------------------------------------
    // Finders — all use PreparedStatements
    // -------------------------------------------------------------------------

    /**
     * Returns CloseableResult — use in try-with-resources.
     *
     *   try (DataStore.CloseableResult cr = CustomerModel.findAll()) {
     *       ui.setData(table, cr, true);
     *   }
     */
    public static DataStore.CloseableResult findAll() {
        return DataStore.getInstance().query("SELECT * FROM customers ORDER BY name");
    }

    /**
     * Load a single customer by ID — safe PreparedStatement.
     */
    public static CustomerModel findById(int id) {
        try (DataStore.CloseableResult cr =
                DataStore.getInstance().queryPrepared(
                    "SELECT * FROM customers WHERE id=?", id)) {
            return fromResultSet(cr.getResultSet());
        } catch (Exception e) {
            MessageLog.getInstance().error("[CustomerModel] findById: " + e.getMessage());
            return null;
        }
    }

    /**
     * Search by name — safe LIKE with PreparedStatement.
     * Returns CloseableResult — use in try-with-resources.
     */
    public static DataStore.CloseableResult findByName(String name) {
        return DataStore.getInstance().queryPrepared(
            "SELECT * FROM customers WHERE name LIKE ?", "%" + name + "%");
    }

    public static CustomerModel fromResultSet(ResultSet rs) {
        if (rs == null) return null;
        try {
            if (rs.next()) {
                CustomerModel c = new CustomerModel();
                c.setCustomerId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setFullName(rs.getString("full_name"));
                c.setEmail(rs.getString("email"));
                c.setHandPhoneNumber(rs.getString("phone"));
                c.setCity(rs.getString("city"));
                c.setCountry(rs.getString("country"));
                c.setDateJoined(rs.getString("date_joined"));
                c.setActive(rs.getInt("active") == 1);
                c.setCustomerNote(rs.getString("note"));
                return c;
            }
        } catch (SQLException e) {
            MessageLog.getInstance().error("[CustomerModel] fromResultSet: " + e.getMessage());
        }
        return null;
    }
}
