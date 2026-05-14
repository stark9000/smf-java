package smf.model;

import smf.database.DataStore;
import smf.events.AppEvent;
import smf.events.AppEventBus;
import smf.messages.MessageLog;
import smf.model.structures.Customer;
import smf.util.AppUtils;

import java.sql.ResultSet;

/**
 * SMF Library - CustomerModel
 *
 * Complete implementation of Customer (which extends Person).
 * Extends AbstractPerson so all 30+ Person fields are inherited
 * without duplication.
 *
 * Also provides save(), load(), and delete() methods that
 * talk directly to DataStore — so the domain object knows
 * how to persist itself.
 *
 * Usage:
 *   CustomerModel c = new CustomerModel();
 *   c.setName("John");
 *   c.setEmail("john@example.com");
 *   c.setCity("Kandy");
 *   c.setCountry("Sri Lanka");
 *   c.save();   // inserts into DB, fires DB_INSERT event
 *
 *   CustomerModel loaded = CustomerModel.findById(1);
 *   System.out.println(loaded.getName());
 *
 * Original concept by: stark (Customerx — most Customer methods
 * threw UnsupportedOperationException)
 * Modernized for Java 8 — all methods implemented, DB integration added
 */
public class CustomerModel extends AbstractPerson implements Customer {

    private final DataStore    db  = DataStore.getInstance();
    private final MessageLog   log = MessageLog.getInstance();
    private final AppEventBus  bus = AppEventBus.getInstance();

    // --- Customer fields ---
    private int     customerId       = -1;
    private String  dateJoined       = "";
    private boolean active           = true;
    private String  customerNote     = "";

    // --- Financial ---
    private String   creditCardNumber = "";
    private String   creditCardType   = "";
    private boolean  creditCardActive = false;
    private String   pin              = "";
    private double   holdings         = 0.0;
    private double[] latePayments     = {};

    public CustomerModel() {
        this.dateJoined = AppUtils.getDate();
    }

    // -------------------------------------------------------------------------
    // Customer interface implementation
    // -------------------------------------------------------------------------

    @Override public void    setCustomerId(int id)         { this.customerId = id; }
    @Override public int     getCustomerId()               { return customerId; }

    @Override public void    setDateJoined(String date)    { this.dateJoined = date; }
    @Override public String  getDateJoined()               { return dateJoined; }

    @Override public void    setActive(boolean active)     { this.active = active; }
    @Override public boolean isActive()                    { return active; }

    @Override public void    setCustomerNote(String note)  { this.customerNote = note; }
    @Override public String  getCustomerNote()             { return customerNote; }

    @Override public void    setCreditCardNumber(String n) { this.creditCardNumber = n; }
    @Override public String  getCreditCardNumber()         { return creditCardNumber; }

    @Override public void    setCreditCardType(String t)   { this.creditCardType = t; }
    @Override public String  getCreditCardType()           { return creditCardType; }

    @Override public void    setCreditCardActive(boolean a){ this.creditCardActive = a; }
    @Override public boolean isCreditCardActive()          { return creditCardActive; }

    @Override public void    setPin(String pin)            { this.pin = pin; }
    @Override public String  getPin()                      { return pin; }

    @Override public void    setHoldings(double h)         { this.holdings = h; }
    @Override public double  getHoldings()                 { return holdings; }

    @Override public void     setLatePayments(double[] p)  { this.latePayments = p; }
    @Override public double[] getLatePayments()             { return latePayments; }

    // -------------------------------------------------------------------------
    // Database operations
    // -------------------------------------------------------------------------

    /**
     * Save this customer to the database.
     * If customerId == -1, performs INSERT (new customer).
     * Otherwise performs UPDATE (existing customer).
     *
     * @return true if saved successfully.
     */
    public boolean save() {
        if (customerId == -1) {
            return insert();
        } else {
            return update();
        }
    }

    private boolean insert() {
        boolean ok = db.Insert(
            "INSERT INTO customers (name, full_name, email, phone, city, country, date_joined, active, note)",
            new String[]{
                getName(), getFullName(), getEmail(),
                getHandPhoneNumber(), getCity(), getCountry(),
                dateJoined, active ? "1" : "0", customerNote
            }
        );
        if (ok) {
            log.info("[CustomerModel] Saved: " + getName());
            bus.publish(AppEvent.DB_INSERT);
        } else {
            log.error("[CustomerModel] Insert failed for: " + getName());
            bus.publish(AppEvent.DB_ERROR);
        }
        return ok;
    }

    private boolean update() {
        boolean ok = db.Update("customers",
            "name='"         + getName()            + "'" +
            ", full_name='"  + getFullName()         + "'" +
            ", email='"      + getEmail()            + "'" +
            ", phone='"      + getHandPhoneNumber()  + "'" +
            ", city='"       + getCity()             + "'" +
            ", country='"    + getCountry()          + "'" +
            ", active="      + (active ? "1" : "0") +
            ", note='"       + customerNote          + "'",
            "id = " + customerId
        );
        if (ok) {
            log.info("[CustomerModel] Updated id=" + customerId);
            bus.publish(AppEvent.DB_UPDATE);
        } else {
            log.error("[CustomerModel] Update failed for id=" + customerId);
            bus.publish(AppEvent.DB_ERROR);
        }
        return ok;
    }

    /**
     * Delete this customer from the database.
     *
     * @return true if deleted successfully.
     */
    public boolean delete() {
        if (customerId == -1) return false;
        boolean ok = db.Delete("customers", "id", String.valueOf(customerId));
        if (ok) {
            log.info("[CustomerModel] Deleted id=" + customerId);
            bus.publish(AppEvent.DB_DELETE);
        }
        return ok;
    }

    // -------------------------------------------------------------------------
    // Static finders
    // -------------------------------------------------------------------------

    /**
     * Load a customer from the database by ID.
     *
     * @param id The customer ID.
     * @return Populated CustomerModel, or null if not found.
     */
    public static CustomerModel findById(int id) {
        DataStore db = DataStore.getInstance();
        ResultSet rs = db.Select("customers", "*", "id = " + id, true);
        return fromResultSet(rs);
    }

    /**
     * Load all customers from the database.
     * Returns a ResultSet — pass to UIFill.setData() for JTable display.
     */
    public static ResultSet findAll() {
        return DataStore.getInstance().Select("customers", "*", null, false);
    }

    /**
     * Search customers by name (partial match).
     */
    public static ResultSet findByName(String name) {
        return DataStore.getInstance()
            .Select("customers", "*", "name LIKE '%" + name + "%'", true);
    }

    /**
     * Populate a CustomerModel from the current row of a ResultSet.
     */
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
        } catch (Exception e) {
            MessageLog.getInstance().error("[CustomerModel] fromResultSet: " + e.getMessage());
        }
        return null;
    }
}
