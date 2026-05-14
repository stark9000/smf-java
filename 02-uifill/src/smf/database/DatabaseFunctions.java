package smf.database;

import java.sql.ResultSet;

/**
 * SMF Library - DatabaseFunctions
 *
 * Defines standard CRUD database operations.
 * Implementations handle the actual SQL execution.
 *
 * Naming convention used here:
 *   Select() - read data
 *   Insert() - add new data
 *   Update() - modify existing data
 *   Delete() - remove data
 *   execute() - run raw SQL (use carefully)
 *
 * Original concept by: stark (originally named DatabaseFunctions)
 * Modernized for Java 8
 */
public interface DatabaseFunctions {

    /**
     * Execute a raw SQL query and return results.
     * Use for SELECT statements.
     *
     * @param sql Full SQL query string.
     * @return ResultSet or null on failure.
     * @throws Exception on SQL error.
     */
    ResultSet execute(String sql) throws Exception;

    /**
     * Execute a raw INSERT, UPDATE, or DELETE statement.
     *
     * @param sql Full SQL statement.
     * @return true if successful, false otherwise.
     * @throws Exception on SQL error.
     */
    boolean executeUpdate(String sql) throws Exception;

    /**
     * Insert a row using a prepared statement.
     * Protects against SQL injection.
     *
     * Example:
     *   Insert("INSERT INTO customers (name, email)", new String[]{"John", "john@email.com"});
     *
     * @param sql    SQL string up to but not including VALUES clause.
     * @param params Array of String values to bind.
     * @return true if insert succeeded.
     */
    boolean Insert(String sql, String[] params);

    /**
     * Select rows from a table.
     *
     * Example without WHERE:
     *   Select("customers", "*", null, false)
     *   → SELECT * FROM customers
     *
     * Example with WHERE:
     *   Select("customers", "name, email", "id = 5", true)
     *   → SELECT name, email FROM customers WHERE id = 5
     *
     * @param tableName  Table to query.
     * @param columns    Columns to select (e.g. "*" or "name, email").
     * @param condition  WHERE clause (without the word WHERE), ignored if where=false.
     * @param where      Whether to apply the WHERE condition.
     * @return ResultSet or null on failure.
     */
    ResultSet Select(String tableName, String columns, String condition, boolean where);

    /**
     * Delete rows from a table matching a condition.
     *
     * Example:
     *   Delete("customers", "id", "5")
     *   → DELETE FROM customers WHERE id = 5
     *
     * @param tableName Table to delete from.
     * @param column    Column name for WHERE clause.
     * @param value     Value to match.
     * @return true if delete succeeded.
     */
    boolean Delete(String tableName, String column, String value);

    /**
     * Update a row in a table.
     *
     * Example:
     *   Update("customers", "name = 'Jane'", "id = 5")
     *   → UPDATE customers SET name = 'Jane' WHERE id = 5
     *
     * @param tableName  Table to update.
     * @param setClause  SET clause (without the word SET).
     * @param condition  WHERE clause (without the word WHERE).
     * @return true if update succeeded.
     */
    boolean Update(String tableName, String setClause, String condition);
}
