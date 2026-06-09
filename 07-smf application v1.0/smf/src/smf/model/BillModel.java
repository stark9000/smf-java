package smf.model;

import smf.database.DataStore;
import smf.messages.MessageLog;
import smf.util.AppUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * SMF - BillModel
 *
 * Fixes vs tutorial version:
 *   - save() uses PreparedStatements for both header and line items
 *   - findByInvoiceNumber uses queryPrepared — no string concatenation
 *   - findAll returns CloseableResult
 *   - Line items also loaded via queryPrepared
 */
public class BillModel {

    // -------------------------------------------------------------------------
    // BillItem
    // -------------------------------------------------------------------------

    public static class BillItem {
        private final int    productId;
        private final String productName;
        private final int    quantity;
        private final double unitPrice;

        public BillItem(int productId, String productName, int quantity, double unitPrice) {
            this.productId   = productId;
            this.productName = productName;
            this.quantity    = quantity;
            this.unitPrice   = unitPrice;
        }

        public int    getProductId()   { return productId; }
        public String getProductName() { return productName; }
        public int    getQuantity()    { return quantity; }
        public double getUnitPrice()   { return unitPrice; }
        public double getLineTotal()   { return quantity * unitPrice; }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final DataStore  db  = DataStore.getInstance();
    private final MessageLog log = MessageLog.getInstance();

    private String invoiceNumber  = "INV-" + UUID.randomUUID().toString()
                                              .substring(0, 8).toUpperCase();
    private int    customerId     = -1;
    private String customerName   = "";
    private String paymentDate    = AppUtils.getDate();
    private String currency       = "USD";
    private double deliveryCharge = 0.0;
    private double taxRate        = 0.0;
    private String status         = "PENDING";

    private final List<BillItem> items = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getInvoiceNumber()          { return invoiceNumber; }
    public void   setCustomerId(int id)       { this.customerId = id; }
    public int    getCustomerId()             { return customerId; }
    public void   setCustomerName(String n)   { this.customerName = n; }
    public String getCustomerName()           { return customerName; }
    public void   setPaymentDate(String d)    { this.paymentDate = d; }
    public String getPaymentDate()            { return paymentDate; }
    public void   setCurrency(String c)       { this.currency = c; }
    public String getCurrency()               { return currency; }
    public void   setDeliveryCharge(double d) { this.deliveryCharge = d; }
    public double getDeliveryCharge()         { return deliveryCharge; }
    public void   setTaxRate(double r)        { this.taxRate = r; }
    public double getTaxRate()                { return taxRate; }
    public void   setStatus(String s)         { this.status = s; }
    public String getStatus()                 { return status; }

    public void addItem(int productId, String name, int qty, double price) {
        items.add(new BillItem(productId, name, qty, price));
    }

    public List<BillItem> getItems()  { return Collections.unmodifiableList(items); }
    public void           clearItems(){ items.clear(); }

    public double getSubTotal()   { return items.stream().mapToDouble(BillItem::getLineTotal).sum(); }
    public double getTaxAmount()  { return getSubTotal() * taxRate; }
    public double getTotal()      { return getSubTotal() + getTaxAmount() + deliveryCharge; }

    // -------------------------------------------------------------------------
    // Database — all PreparedStatements
    // -------------------------------------------------------------------------

    public boolean save() {
        // Save bill header
        boolean ok = db.insert(
            "INSERT INTO bills (invoice_number, customer_id, customer_name, " +
            "payment_date, currency, delivery_charge, tax_rate, total, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            invoiceNumber, customerId, customerName,
            paymentDate, currency,
            deliveryCharge, taxRate, getTotal(), status
        );

        if (!ok) {
            log.error("[BillModel] Failed to save bill header: " + invoiceNumber);
            return false;
        }

        // Save line items — each with its own PreparedStatement call
        for (BillItem item : items) {
            db.insert(
                "INSERT INTO bill_items " +
                "(invoice_number, product_id, product_name, quantity, unit_price, line_total) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                invoiceNumber, item.getProductId(), item.getProductName(),
                item.getQuantity(), item.getUnitPrice(), item.getLineTotal()
            );
        }

        log.info("[BillModel] Saved " + invoiceNumber
               + " — " + items.size() + " items, total $" + String.format("%.2f", getTotal()));
        return true;
    }

    // -------------------------------------------------------------------------
    // Finders — all PreparedStatements, CloseableResult for multi-row
    // -------------------------------------------------------------------------

    /** Use in try-with-resources. */
    public static DataStore.CloseableResult findAll() {
        return DataStore.getInstance().query(
            "SELECT * FROM bills ORDER BY payment_date DESC");
    }

    /**
     * Load a full bill with line items — safe PreparedStatement.
     */
    public static BillModel findByInvoiceNumber(String invoiceNumber) {
        DataStore db = DataStore.getInstance();

        try (DataStore.CloseableResult cr = db.queryPrepared(
                "SELECT * FROM bills WHERE invoice_number=?", invoiceNumber)) {

            ResultSet rs = cr.getResultSet();
            if (rs == null || !rs.next()) return null;

            BillModel bill = new BillModel();
            bill.invoiceNumber  = rs.getString("invoice_number");
            bill.customerId     = rs.getInt("customer_id");
            bill.customerName   = rs.getString("customer_name");
            bill.paymentDate    = rs.getString("payment_date");
            bill.currency       = rs.getString("currency");
            bill.deliveryCharge = rs.getDouble("delivery_charge");
            bill.taxRate        = rs.getDouble("tax_rate");
            bill.status         = rs.getString("status");

            // Load line items — safe PreparedStatement
            try (DataStore.CloseableResult itemsCr = db.queryPrepared(
                    "SELECT * FROM bill_items WHERE invoice_number=?", invoiceNumber)) {

                ResultSet itemRs = itemsCr.getResultSet();
                if (itemRs != null) {
                    while (itemRs.next()) {
                        bill.addItem(
                            itemRs.getInt("product_id"),
                            itemRs.getString("product_name"),
                            itemRs.getInt("quantity"),
                            itemRs.getDouble("unit_price")
                        );
                    }
                }
            }

            return bill;

        } catch (SQLException e) {
            MessageLog.getInstance().error("[BillModel] findByInvoiceNumber: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        return "Bill[" + invoiceNumber + ", " + customerName
             + ", $" + String.format("%.2f", getTotal()) + ", " + status + "]";
    }
}
