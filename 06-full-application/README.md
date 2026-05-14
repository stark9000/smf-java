# SMF Tutorial 06 — Full Application (The Finale)

## What this is

A complete, working shop application that wires together every concept
from Tutorials 01–05 into one cohesive system.

This is what the original `smf-lib` and `shop/shade` project were
always building toward — now finished.

Original code by: **stark**
Modernized and completed for **Java 8**

---

## What's in the application

```
Five tabs, all decoupled through AppEventBus:

📊 Dashboard   — live counts, low stock alerts, real-time event log
🛒 Sales       — browse products, build cart, checkout → saves bill
📦 Products    — add/delete products, live filter
👥 Customers   — add/delete customers
🧾 Bills       — view all invoices, double-click to see full invoice
```

---

## File structure

```
src/
├── app/
│   ├── Main.java                    ← entry point + splash screen
│   ├── MainWindow.java              ← tabbed frame, status bar, clock
│   └── panels/
│       ├── DashboardPanel.java      ← stats + low stock + event log
│       ├── SalesPanel.java          ← cart + checkout + invoice
│       ├── ProductsPanel.java       ← product table + add form
│       ├── CustomersPanel.java      ← customer table + add form
│       └── BillsPanel.java         ← bills table + invoice viewer
└── smf/                            ← full library from Tutorials 01–05
    ├── database/
    ├── events/
    ├── loader/
    ├── messages/
    ├── model/
    ├── uifill/
    └── util/
```

---

## How to compile and run

```bash
javac -cp lib/mysql-connector-java.jar -sourcepath src -d out src/app/Main.java
java  -cp out:lib/mysql-connector-java.jar app.Main
```

On Windows use `;` instead of `:`.

**Runs without a database** — the splash screen will warn about
DB connection failure, but the app launches in demo mode.
All UI works; save/load operations log errors gracefully.

---

## Database setup

```sql
CREATE DATABASE smf_shop;
USE smf_shop;

CREATE TABLE products (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(100),
    category         VARCHAR(50),
    country_of_origin VARCHAR(80),
    supplier_id      INT DEFAULT -1,
    min_stock        INT DEFAULT 0,
    current_stock    INT DEFAULT 0,
    purchase_price   DECIMAL(10,2),
    sales_price      DECIMAL(10,2),
    rent_price       DECIMAL(10,2) DEFAULT 0,
    description      TEXT
);

CREATE TABLE customers (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100),
    full_name   VARCHAR(150),
    email       VARCHAR(100),
    phone       VARCHAR(30),
    city        VARCHAR(80),
    country     VARCHAR(80),
    date_joined DATE,
    active      TINYINT DEFAULT 1,
    note        TEXT
);

CREATE TABLE bills (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    invoice_number  VARCHAR(30) UNIQUE,
    customer_id     INT DEFAULT -1,
    customer_name   VARCHAR(100),
    payment_date    DATE,
    currency        VARCHAR(10) DEFAULT 'USD',
    delivery_charge DECIMAL(10,2) DEFAULT 0,
    tax_rate        DECIMAL(5,4) DEFAULT 0,
    total           DECIMAL(10,2),
    status          VARCHAR(20) DEFAULT 'PENDING'
);

CREATE TABLE bill_items (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(30),
    product_id     INT DEFAULT -1,
    product_name   VARCHAR(100),
    quantity       INT,
    unit_price     DECIMAL(10,2),
    line_total     DECIMAL(10,2)
);

-- Sample products
INSERT INTO products (name, category, purchase_price, sales_price, current_stock, min_stock)
VALUES
  ('Arduino Nano',   'Microcontrollers', 2.50, 4.50,  100, 20),
  ('ESP32 DevKit',   'Microcontrollers', 4.50, 6.80,  50,  10),
  ('STM32F103C8T6',  'Microcontrollers', 2.80, 5.00,  75,  15),
  ('DHT22 Sensor',   'Sensors',          1.20, 2.50,  200, 30),
  ('OLED 0.96"',     'Displays',         1.50, 3.20,  60,  10),
  ('WS2812B Strip',  'LEDs',             3.00, 5.50,  5,   10);
```

Update `app.properties` (auto-created on first run):

```properties
db.host=127.0.0.1
db.port=3306
db.name=smf_shop
db.user=root
db.password=yourpassword
app.title=SMF Shop
app.version=1.0
```

---

## How the pieces connect

```
                    ┌─────────────────────────────┐
                    │         AppEventBus          │
                    │   (Tutorial 03 singleton)    │
                    └──────────────┬──────────────┘
                                   │ publish / subscribe
          ┌────────────────────────┼────────────────────────┐
          ▼                        ▼                         ▼
   DashboardPanel           ProductsPanel             SalesPanel
   implements               implements                implements
   EventWatcher             EventWatcher              EventWatcher
   refresh()                refresh()                 refresh()
          ▲                        ▲                         ▲
          └────────────────────────┼────────────────────────┘
                                   │ fires DB_INSERT / DB_DELETE
                    ┌──────────────┴──────────────┐
                    │          DataStore           │
                    │   (Tutorial 01 singleton)    │
                    └─────────────────────────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │    CustomerModel             │
                    │    ProductModel    (T05)     │
                    │    BillModel                 │
                    └─────────────────────────────┘
```

**Key property:** None of the panels know about each other.
SalesPanel doesn't call `dashboard.refresh()` or `products.refresh()`.
It calls `bill.save()` → DataStore fires event → AppEventBus delivers
to all subscribers → each panel refreshes itself.

---

## The Sales flow (Tutorial 02 + 05 working together)

```
1. UIFill.setData(productsTable, ProductModel.findAll(), true)
   → JTable populated from DB ResultSet, sortable

2. User selects product + sets quantity → "Add to Cart"
   → Row data read from table model → added to cart DefaultTableModel

3. Customer selected from JComboBox (also filled by UIFill from DB)

4. "Checkout" button:
   BillModel bill = new BillModel();
   bill.addItem(...) for each cart row
   bill.save()          → DataStore.Insert() fires
                        → AppEventBus.publish(DB_INSERT)
                        → ALL EventWatcher panels refresh()

5. Invoice dialog shown
6. Cart cleared
```

---

## What was completed vs the original

| Original smf-lib / shop | This application |
|---|---|
| `NewJFrame` — one giant class doing everything | 5 focused panels, each self-contained |
| `update()` just printed to console | `update()` drives real UI refresh |
| `Customerx` — Customer methods unimplemented | `CustomerModel` — fully implemented |
| `Billx` — all methods unimplemented | `BillModel` — complete with line items |
| `PropertyFile` — never implemented | `AppConfig` — full .properties load/save |
| `Extrax` — most methods unimplemented | `AppUtils` — all methods complete |
| No splash screen | Animated splash with real progress |
| Hardcoded DB settings | `app.properties` config file |
| Raw string events `"--insert"` | Typed `AppEvent` constants |
| Single main frame | Tabbed application, status bar, live clock |
| No invoice generation | Full invoice with line items and totals |
| Loading on EDT (could freeze) | `SwingWorker` background loading |

---

## Complete tutorial series

| # | Topic | Key concept |
|---|---|---|
| 01 | Database Connection | DataStore, interfaces, singleton |
| 02 | UIFill | JTable/JComboBox/JList from ResultSet |
| 03 | EventWatcher & MessageCenter | Observer pattern, event bus |
| 04 | Loader & Bootstrap | Startup sequence, config, splash |
| 05 | Domain Model | Person hierarchy, AbstractPerson |
| **06** | **Full Application** | **Everything wired together** |

---

*Built from the original smf-lib (2012–2013) by stark.*
*Modernized and completed 2024–2025.*
