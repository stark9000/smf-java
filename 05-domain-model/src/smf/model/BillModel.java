package smf.model;

import smf.database.DataStore;
import smf.events.AppEvent;
import smf.events.AppEventBus;
import smf.messages.MessageLog;
import smf.util.AppUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SMF Library - BillModel
 *
 * Complete implementation of a Bill/Invoice.
 *
 * In the original smf-lib, Billx had a start on setInvoiceNumber()
 * (reading from DB then string-concatenating "1" — which would produce
 * IDs like "11", "21", "31"...) and all other methods threw
 * UnsupportedOperationException.
 *
 * BillModel completes the implementation:
 *   - Invoice number generated as UUID prefix (unique, no DB read needed)
 *   - Line items tracked as a List<BillItem>
 *   - Total, tax, delivery charge calculated
 *   - Full DB save/load
 *
 * Original concept by: stark (Billx)
 * Modernized for Java 8
 */
public class BillModel {

    // -------------------------------------------------------------------------
    // BillItem — one line on the invoice
    // -------------------------------------------------------------------------

    public static class BillItem {

        private final int    productId;
        private final String productName;
        private final int    quantity;
        private final double unitPrice;

        public BillItem(int productId, String productName,
                        int quantity, double unitPrice) {
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

        @Override
        public String toString() {
            return productName + " x" + quantity
                 + " @ $" + unitPrice + " = $" + getLineTotal();
        }
    }

    // -------------------------------------------------------------------------
    // BillModel fields
    // -------------------------------------------------------------------------

    private final DataStore   db  = DataStore.getInstance();
    private final MessageLog  log = MessageLog.getInstance();
    private final AppEventBus bus = AppEventBus.getInstance();

    private String invoiceNumber  = "";
    private int    customerId     = -1;
    private String customerName   = "";
    private String paymentDate    = "";
    private String currency       = "USD";
    private double deliveryCharge = 0.0;
    private double taxRate        = 0.0;   // e.g. 0.08 for 8%
    private String status         = "PENDING"; // PENDING, PAID, CANCELLED

    private final List<BillItem> items = new ArrayList<>();

    public BillModel() {
        // Generate a unique invoice number — fixes the original Billx bug
        // where concatenating "1" to the last ID produced "11", "21" etc.
        this.invoiceNumber = "INV-"
            + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.paymentDate = AppUtils.getDate();
    }

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

    public void   setTaxRate(double rate)     { this.taxRate = rate; }
    public double getTaxRate()                { return taxRate; }

    public void   setStatus(String s)         { this.status = s; }
    public String getStatus()                 { return status; }

    // -------------------------------------------------------------------------
    // Line items
    // -------------------------------------------------------------------------

    public void addItem(BillItem item) {
        items.add(item);
    }

    public void addItem(int productId, String name, int qty, double price) {
        items.add(new BillItem(productId, name, qty, price));
    }

    public List<BillItem> getItems() {
        return new ArrayList<>(items);
    }

    public void clearItems() {
        items.clear();
    }

    // -------------------------------------------------------------------------
    // Calculated totals
    // -------------------------------------------------------------------------

    /** Sum of all line item totals. */
    public double getSubTotal() {
        return items.stream().mapToDouble(BillItem::getLineTotal).sum();
    }

    /** Tax amount based on taxRate. */
    public double getTaxAmount() {
        return getSubTotal() * taxRate;
    }

    /** Grand total: subtotal + tax + delivery. */
    public double getTotal() {
        return getSubTotal() + getTaxAmount() + deliveryCharge;
    }

    // -------------------------------------------------------------------------
    // Database operations
    // -------------------------------------------------------------------------

    /**
     * Save the bill and all its line items to the database.
     */
    public boolean save() {
        // Save bill header
        boolean ok = db.Insert(
            "INSERT INTO bills (invoice_number, customer_id, customer_name, " +
            "payment_date, currency, delivery_charge, tax_rate, total, status)",
            new String[]{
                invoiceNumber, String.valueOf(customerId), customerName,
                paymentDate, currency,
                String.valueOf(deliveryCharge), String.valueOf(taxRate),
                String.valueOf(getTotal()), status
            }
        );

        if (!ok) {
            log.error("[BillModel] Failed to save bill header: " + invoiceNumber);
            bus.publish(AppEvent.DB_ERROR);
            return false;
        }

        // Save each line item
        for (BillItem item : items) {
            db.Insert(
                "INSERT INTO bill_items (invoice_number, product_id, product_name, quantity, unit_price, line_total)",
                new String[]{
                    invoiceNumber, String.valueOf(item.getProductId()),
                    item.getProductName(), String.valueOf(item.getQuantity()),
                    String.valueOf(item.getUnitPrice()), String.valueOf(item.getLineTotal())
                }
            );
        }

        log.info("[BillModel] Saved invoice " + invoiceNumber
               + " — " + items.size() + " items, total $" + getTotal());
        bus.publish(AppEvent.DB_INSERT);
        return true;
    }

    /**
     * Load a bill by invoice number.
     */
    public static BillModel findByInvoiceNumber(String invoiceNumber) {
        DataStore db = DataStore.getInstance();
        ResultSet rs = db.Select("bills", "*",
            "invoice_number = '" + invoiceNumber + "'", true);
        if (rs == null) return null;

        try {
            if (rs.next()) {
                BillModel bill = new BillModel();
                bill.invoiceNumber  = rs.getString("invoice_number");
                bill.customerId     = rs.getInt("customer_id");
                bill.customerName   = rs.getString("customer_name");
                bill.paymentDate    = rs.getString("payment_date");
                bill.currency       = rs.getString("currency");
                bill.deliveryCharge = rs.getDouble("delivery_charge");
                bill.taxRate        = rs.getDouble("tax_rate");
                bill.status         = rs.getString("status");

                // Load line items
                ResultSet items = db.Select("bill_items", "*",
                    "invoice_number = '" + invoiceNumber + "'", true);
                if (items != null) {
                    while (items.next()) {
                        bill.addItem(
                            items.getInt("product_id"),
                            items.getString("product_name"),
                            items.getInt("quantity"),
                            items.getDouble("unit_price")
                        );
                    }
                }
                return bill;
            }
        } catch (Exception e) {
            MessageLog.getInstance().error("[BillModel] findByInvoiceNumber: " + e.getMessage());
        }
        return null;
    }

    public static ResultSet findAll() {
        return DataStore.getInstance().Select("bills", "*", null, false);
    }

    @Override
    public String toString() {
        return "Bill[" + invoiceNumber
             + ", customer=" + customerName
             + ", items="    + items.size()
             + ", total=$"   + String.format("%.2f", getTotal())
             + ", status="   + status + "]";
    }
}
