package smf.model.structures;

/**
 * SMF Library - Customer
 *
 * Extends Person with customer-specific fields.
 * A Customer IS a Person — same address, contact, identity fields —
 * plus customer account details.
 *
 * Original concept by: stark
 * Modernized for Java 8 — cleaned up method signatures,
 * removed inconsistent parameter styles from original
 */
public interface Customer extends Person {

    void   setCustomerId(int id);
    int    getCustomerId();

    void   setDateJoined(String date);   // format: yyyy-MM-dd
    String getDateJoined();

    void   setActive(boolean active);
    boolean isActive();

    void   setCustomerNote(String note);
    String getCustomerNote();

    // --- Financial ---
    void   setCreditCardNumber(String number);
    String getCreditCardNumber();

    void   setCreditCardType(String type);   // VISA, MASTERCARD, etc.
    String getCreditCardType();

    void   setCreditCardActive(boolean active);
    boolean isCreditCardActive();

    void   setPin(String pin);
    String getPin();

    void   setHoldings(double holdings);
    double getHoldings();

    void     setLatePayments(double[] payments);
    double[] getLatePayments();
}
