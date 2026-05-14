# SMF Tutorial 02 — UIFill

## What this covers

How to populate Swing components (`JTable`, `JComboBox`, `JList`) directly
from a database `ResultSet` or a plain Java `List` — with filtering,
row copying between tables, and cell-edit detection.

Based on the original `smf-lib` UIfill layer by **stark**, modernized for **Java 8**.

---

## Files

```
src/
├── smf/
│   ├── uifill/
│   │   ├── TableFill.java        ← interface: JTable fill contract
│   │   ├── ComboFill.java        ← interface: JComboBox fill contract
│   │   ├── ListFill.java         ← interface: JList fill contract
│   │   ├── UIFill.java           ← implementation: all three combined
│   │   └── TableCellEditor.java  ← detects cell edits, fires lambda callback
│   ├── database/                 ← from Tutorial 01
│   ├── events/                   ← from Tutorial 01
│   └── messages/                 ← from Tutorial 01
└── demo/
    └── UIFillDemo.java           ← runnable Swing demo window
```

---

## Requirements

- Java 8+
- MySQL (optional — demo also works with plain List data)
- `mysql-connector-java.jar` in classpath

---

## Database setup (optional)

```sql
CREATE DATABASE smf_test;
USE smf_test;

CREATE TABLE products (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100),
    category VARCHAR(50),
    price    DECIMAL(10,2),
    stock    INT
);

CREATE TABLE categories (
    id   INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50)
);

CREATE TABLE suppliers (
    id   INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100)
);

-- Sample data
INSERT INTO categories (name) VALUES ('Microcontrollers'), ('Sensors'), ('Displays'), ('Power');
INSERT INTO suppliers  (name) VALUES ('DigiKey'), ('Mouser'), ('LCSC'), ('RS Components');
INSERT INTO products   (name, category, price, stock) VALUES
    ('Arduino Nano',  'Microcontrollers', 4.50,  100),
    ('ESP32 DevKit',  'Microcontrollers', 6.80,  50),
    ('STM32F103',     'Microcontrollers', 3.20,  75),
    ('DHT22',         'Sensors',          2.10,  200),
    ('OLED 0.96"',    'Displays',         3.50,  60);
```

---

## How to compile and run

```bash
javac -cp lib/mysql-connector-java.jar -sourcepath src -d out src/demo/UIFillDemo.java
java  -cp out:lib/mysql-connector-java.jar demo.UIFillDemo
```

On Windows use `;` instead of `:`.

To run **without a database** — just click **"Load from List"** in the demo window.

---

## Key concepts

### Fill JTable from ResultSet

```java
UIFill ui = new UIFill();
DataStore db = DataStore.getInstance();

ResultSet rs = db.Select("products", "*", null, false);
ui.setData(myTable, rs, true);  // true = enable column sorting
```
Column names are taken automatically from the ResultSet metadata.

---

### Fill JTable from a List

```java
ui.setData(myTable,
    Arrays.asList("Arduino", "ESP32", "STM32"),
    new Object[]{"Product Name"}
);
```

---

### Fill JComboBox from ResultSet

```java
ResultSet rs = db.Select("categories", "name", null, false);
ui.setData(myCombo, rs, "name");
```

Items are sorted alphabetically automatically.

---

### Fill JComboBox from a List

```java
ui.setData(myCombo, Arrays.asList("DigiKey", "Mouser", "LCSC"));
```

---

### Fill JList from ResultSet

```java
ResultSet rs = db.Select("suppliers", "name", null, false);
ui.setData(myList, rs, "name");
```

---

### Filter a JTable

```java
ui.setFilter(myTable, "Arduino");  // show only rows containing "Arduino"
ui.setFilter(myTable, "");          // clear filter, show all
```
Supports regex. Case-insensitive by default.

---

### Copy selected rows between JTables

```java
ui.copySelectedRows(sourceTable, targetTable);
```
Works with single, multiple, or all rows selected.
Duplicates are automatically skipped.

---

### Detect cell edits (Java 8 lambda)

```java
new TableCellEditor(myTable, edit -> {
    System.out.println("Row: "     + edit.getRow());
    System.out.println("Column: "  + edit.getColumn());
    System.out.println("Old: "     + edit.getOldValue());
    System.out.println("New: "     + edit.getNewValue());

    // Save back to database
    db.Update("products",
        "price = '" + edit.getNewValue() + "'",
        "id = "     + getIdForRow(edit.getRow())
    );
});
```

---

## What changed from the original smf-lib

| Original | Modernized |
|---|---|
| `jTableFill`, `jComboBoxFill`, `jListFill` separate interfaces | `TableFill`, `ComboFill`, `ListFill` — cleaner names |
| Implemented directly in `NewJFrame` | Separate `UIFill` class — reusable anywhere |
| Raw `ArrayList`, `TreeSet` | Generics throughout |
| `Action` callback in `jTableEditor` | `Consumer<CellEdit>` lambda in `TableCellEditor` |
| No ResultSet direct-to-combo/list | Added `setData(combo, rs, columnName)` overloads |
| `resultSetToTableModel` had a bug (rowss[1] instead of rowss[i]) | Fixed in `toArrayList` → `setData` pipeline |
| No case-insensitive filter | Filter uses `(?i)` regex prefix |

---

## Next tutorial

**03 — EventWatcher & MessageCenter**
(Observer pattern in practice — wiring UI components to database events)
