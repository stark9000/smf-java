package smf.model;

import smf.database.DataStore;
import smf.messages.MessageLog;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SMF - ProductModel
 *
 * Fixes vs tutorial version:
 *   - update() uses PreparedStatement with ? — no string concatenation
 *   - findAll, findLowStock return CloseableResult
 *   - findById uses queryPrepared — no string concatenation
 *   - findByCategory uses queryPrepared
 */
public class ProductModel {

    private final DataStore  db  = DataStore.getInstance();
    private final MessageLog log = MessageLog.getInstance();

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
    // Getters / Setters
    // -------------------------------------------------------------------------

    public void   setProductId(int id)           { this.productId = id; }
    public int    getProductId()                 { return productId; }
    public void   setName(String v)              { this.name = v; }
    public String getName()                      { return name; }
    public void   setCategory(String v)          { this.category = v; }
    public String getCategory()                  { return category; }
    public void   setCountryOfOrigin(String v)   { this.countryOfOrigin = v; }
    public String getCountryOfOrigin()           { return countryOfOrigin; }
    public void   setSupplierId(int v)           { this.supplierId = v; }
    public int    getSupplierId()                { return supplierId; }
    public void   setMinStock(int v)             { this.minStock = v; }
    public int    getMinStock()                  { return minStock; }
    public void   setCurrentStock(int v)         { this.currentStock = v; }
    public int    getCurrentStock()              { return currentStock; }
    public void   setPurchasePrice(double v)     { this.purchasePrice = v; }
    public double getPurchasePrice()             { return purchasePrice; }
    public void   setSalesPrice(double v)        { this.salesPrice = v; }
    public double getSalesPrice()               { return salesPrice; }
    public void   setRentPrice(double v)         { this.rentPrice = v; }
    public double getRentPrice()                 { return rentPrice; }
    public void   setDescription(String v)       { this.description = v; }
    public String getDescription()              { return description; }

    public double  getMargin()   { return salesPrice - purchasePrice; }
    public boolean isLowStock()  { return currentStock < minStock; }

    // -------------------------------------------------------------------------
    // Database — all PreparedStatements
    // -------------------------------------------------------------------------

    public boolean save() {
        return productId == -1 ? insert() : update();
    }

    private boolean insert() {
        return db.insert(
            "INSERT INTO products " +
            "(name, category, country_of_origin, supplier_id, min_stock, " +
            "current_stock, purchase_price, sales_price, rent_price, description) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            name, category, countryOfOrigin, supplierId,
            minStock, currentStock, purchasePrice, salesPrice, rentPrice, description
        );
    }

    private boolean update() {
        return db.update(
            "UPDATE products SET name=?, category=?, country_of_origin=?, " +
            "min_stock=?, current_stock=?, purchase_price=?, sales_price=?, " +
            "rent_price=?, description=? WHERE id=?",
            name, category, countryOfOrigin,
            minStock, currentStock, purchasePrice, salesPrice, rentPrice,
            description, productId
        );
    }

    public boolean delete() {
        if (productId == -1) return false;
        return db.delete("DELETE FROM products WHERE id=?", productId);
    }

    // -------------------------------------------------------------------------
    // Finders — CloseableResult for all multi-row results
    // -------------------------------------------------------------------------

    /** Use in try-with-resources. */
    public static DataStore.CloseableResult findAll() {
        return DataStore.getInstance().query("SELECT * FROM products ORDER BY name");
    }

    /** Safe — PreparedStatement. Use in try-with-resources. */
    public static DataStore.CloseableResult findByCategory(String category) {
        return DataStore.getInstance().queryPrepared(
            "SELECT * FROM products WHERE category=? ORDER BY name", category);
    }

    /** Use in try-with-resources. */
    public static DataStore.CloseableResult findLowStock() {
        return DataStore.getInstance().query(
            "SELECT * FROM products WHERE current_stock < min_stock ORDER BY name");
    }

    /** Safe — PreparedStatement. Returns single object. */
    public static ProductModel findById(int id) {
        try (DataStore.CloseableResult cr =
                DataStore.getInstance().queryPrepared(
                    "SELECT * FROM products WHERE id=?", id)) {
            return fromResultSet(cr.getResultSet());
        } catch (Exception e) {
            MessageLog.getInstance().error("[ProductModel] findById: " + e.getMessage());
            return null;
        }
    }

    public static ProductModel fromResultSet(ResultSet rs) {
        if (rs == null) return null;
        try {
            if (rs.next()) {
                ProductModel p = new ProductModel();
                p.setProductId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setCategory(rs.getString("category"));
                p.setCountryOfOrigin(rs.getString("country_of_origin"));
                p.setSupplierId(rs.getInt("supplier_id"));
                p.setMinStock(rs.getInt("min_stock"));
                p.setCurrentStock(rs.getInt("current_stock"));
                p.setPurchasePrice(rs.getDouble("purchase_price"));
                p.setSalesPrice(rs.getDouble("sales_price"));
                p.setRentPrice(rs.getDouble("rent_price"));
                p.setDescription(rs.getString("description"));
                return p;
            }
        } catch (SQLException e) {
            MessageLog.getInstance().error("[ProductModel] fromResultSet: " + e.getMessage());
        }
        return null;
    }

    @Override
    public String toString() {
        return "Product[" + productId + ", " + name + ", $" + salesPrice + "]";
    }
}
