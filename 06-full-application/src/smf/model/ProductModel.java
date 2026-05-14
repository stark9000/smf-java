package smf.model;

import smf.database.DataStore;
import smf.events.AppEvent;
import smf.events.AppEventBus;
import smf.messages.MessageLog;

import java.sql.ResultSet;

/**
 * SMF Library - ProductModel
 *
 * Complete implementation of Product with database persistence.
 *
 * Product is standalone — it does not extend Person.
 * It can link to a Supplier via supplierId.
 *
 * Original concept by: stark (Productx — the commented-out Supplier
 * reference is preserved here as supplierId)
 * Modernized for Java 8
 */
public class ProductModel {

    private final DataStore   db  = DataStore.getInstance();
    private final MessageLog  log = MessageLog.getInstance();
    private final AppEventBus bus = AppEventBus.getInstance();

    private int    productId       = -1;
    private String name            = "";
    private String category        = "";
    private String countryOfOrigin = "";
    private int    supplierId      = -1;
    private int    minStock        = 0;
    private int    currentStock    = 0;
    private double purchasePrice   = 0.0;
    private double salesPrice      = 0.0;
    private double rentPrice       = 0.0;
    private String description     = "";

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public void   setProductId(int id)           { this.productId = id; }
    public int    getProductId()                 { return productId; }

    public void   setName(String name)           { this.name = name; }
    public String getName()                      { return name; }

    public void   setCategory(String category)   { this.category = category; }
    public String getCategory()                  { return category; }

    public void   setCountryOfOrigin(String c)   { this.countryOfOrigin = c; }
    public String getCountryOfOrigin()           { return countryOfOrigin; }

    public void   setSupplierId(int id)          { this.supplierId = id; }
    public int    getSupplierId()                { return supplierId; }

    public void   setMinStock(int min)           { this.minStock = min; }
    public int    getMinStock()                  { return minStock; }

    public void   setCurrentStock(int stock)     { this.currentStock = stock; }
    public int    getCurrentStock()              { return currentStock; }

    public void   setPurchasePrice(double price) { this.purchasePrice = price; }
    public double getPurchasePrice()             { return purchasePrice; }

    public void   setSalesPrice(double price)    { this.salesPrice = price; }
    public double getSalesPrice()               { return salesPrice; }

    public void   setRentPrice(double price)     { this.rentPrice = price; }
    public double getRentPrice()                 { return rentPrice; }

    public void   setDescription(String desc)    { this.description = desc; }
    public String getDescription()              { return description; }

    /**
     * Profit margin on this product.
     * salesPrice - purchasePrice
     */
    public double getMargin() {
        return salesPrice - purchasePrice;
    }

    /**
     * True if current stock is below the minimum threshold.
     */
    public boolean isLowStock() {
        return currentStock < minStock;
    }

    // -------------------------------------------------------------------------
    // Database operations
    // -------------------------------------------------------------------------

    public boolean save() {
        if (productId == -1) {
            return insert();
        } else {
            return update();
        }
    }

    private boolean insert() {
        boolean ok = db.Insert(
            "INSERT INTO products (name, category, country_of_origin, supplier_id, " +
            "min_stock, current_stock, purchase_price, sales_price, rent_price, description)",
            new String[]{
                name, category, countryOfOrigin,
                String.valueOf(supplierId),
                String.valueOf(minStock), String.valueOf(currentStock),
                String.valueOf(purchasePrice), String.valueOf(salesPrice),
                String.valueOf(rentPrice), description
            }
        );
        if (ok) {
            log.info("[ProductModel] Saved: " + name);
            bus.publish(AppEvent.DB_INSERT);
        } else {
            log.error("[ProductModel] Save failed: " + name);
            bus.publish(AppEvent.DB_ERROR);
        }
        return ok;
    }

    private boolean update() {
        boolean ok = db.Update("products",
            "name='"            + name             + "'" +
            ", category='"      + category          + "'" +
            ", sales_price="    + salesPrice        +
            ", purchase_price=" + purchasePrice     +
            ", current_stock="  + currentStock      +
            ", description='"   + description       + "'",
            "id = " + productId
        );
        if (ok) {
            log.info("[ProductModel] Updated id=" + productId);
            bus.publish(AppEvent.DB_UPDATE);
        } else {
            log.error("[ProductModel] Update failed id=" + productId);
            bus.publish(AppEvent.DB_ERROR);
        }
        return ok;
    }

    public boolean delete() {
        if (productId == -1) return false;
        boolean ok = db.Delete("products", "id", String.valueOf(productId));
        if (ok) {
            log.info("[ProductModel] Deleted id=" + productId);
            bus.publish(AppEvent.DB_DELETE);
        }
        return ok;
    }

    // -------------------------------------------------------------------------
    // Static finders
    // -------------------------------------------------------------------------

    public static ProductModel findById(int id) {
        ResultSet rs = DataStore.getInstance()
            .Select("products", "*", "id = " + id, true);
        return fromResultSet(rs);
    }

    public static ResultSet findAll() {
        return DataStore.getInstance().Select("products", "*", null, false);
    }

    public static ResultSet findLowStock() {
        return DataStore.getInstance()
            .Select("products", "*", "current_stock < min_stock", true);
    }

    public static ProductModel fromResultSet(ResultSet rs) {
        if (rs == null) return null;
        try {
            if (rs.next()) {
                ProductModel p = new ProductModel();
                p.setProductId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setCategory(rs.getString("category"));
                p.setSalesPrice(rs.getDouble("sales_price"));
                p.setPurchasePrice(rs.getDouble("purchase_price"));
                p.setCurrentStock(rs.getInt("current_stock"));
                p.setMinStock(rs.getInt("min_stock"));
                p.setDescription(rs.getString("description"));
                return p;
            }
        } catch (Exception e) {
            MessageLog.getInstance().error("[ProductModel] fromResultSet: " + e.getMessage());
        }
        return null;
    }

    @Override
    public String toString() {
        return "Product[" + productId + ", " + name
             + ", $" + salesPrice + ", stock=" + currentStock + "]";
    }
}
