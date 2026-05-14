# SMF Library — Java Swing + MySQL Tutorials

A series of six tutorials built from a real Java library written in 2012–2013
while learning Java. The original code (`smf-lib`) was a self-built framework
for desktop shop/business applications — complete with database layer,
observer pattern, UI helpers, and domain models.

This repository modernizes that original work for **Java 8**, completes the
unfinished parts, and documents every concept as a standalone tutorial.

> *"One of the best things I wrote while learning Java."*
> — stark (original author)

---

## What is smf-lib?

A reusable Java library for building Swing desktop applications that talk to MySQL.

It provides:
- A database layer with a clean interface/implementation split
- Helpers to populate `JTable`, `JComboBox`, and `JList` directly from a `ResultSet`
- An observer pattern (EventWatcher) so UI components react to database events
- A bootstrap sequence that loads config and initializes singletons in the right order
- A domain model hierarchy (`Person → Customer / Employee / Supplier`) with DB persistence

The library grew from **v1-0 to v1-6** over about 18 months. Tutorial 06 shows
what a complete application built on it looks like.

---

## Tutorial Series

### [01 — Database Connection](01-database-connection/)
The foundation. `DataStore` singleton, `DatabaseConnection` and `DatabaseFunctions`
interfaces, `EventWatcher` observer pattern, `SystemMessages` log.

```java
DataStore db = DataStore.getInstance();
db.setServerIP("127.0.0.1");
db.setDatabaseName("myshop");

ResultSet rs = db.Select("products", "*", null, false);
db.Insert("INSERT INTO products (name, price)", new String[]{"ESP32", "6.80"});
db.Update("products", "price = 7.00", "name = 'ESP32'");
db.Delete("products", "name", "ESP32");

// React to any DB operation
db.registerWatcher(event -> System.out.println("Event: " + event));
```

---

### [02 — UIFill](02-uifill/)
Populate Swing components directly from database `ResultSet` objects.
Live table filtering, row copying between tables, cell-edit detection.

```java
UIFill ui = new UIFill();

// JTable from DB — column names auto-detected from metadata
ui.setData(myTable, db.Select("products", "*", null, false), true);

// JComboBox from a single DB column
ui.setData(myCombo, db.Select("categories", "name", null, false), "name");

// JList from a single DB column
ui.setData(myList, db.Select("suppliers", "name", null, false), "name");

// Live filter
ui.setFilter(myTable, "Arduino");

// Detect cell edits with a lambda
new TableCellEditor(myTable, edit -> {
    System.out.println("Changed: " + edit.getOldValue() + " → " + edit.getNewValue());
});
```

---

### [03 — EventWatcher & MessageCenter](03-event-message/)
The observer pattern in practice. `AppEventBus` routes events between
components that have no direct references to each other.
`MessageLog` with severity levels (INFO / WARN / ERROR) and timestamps.

```java
AppEventBus bus = AppEventBus.getInstance();

// Subscribe with a class (classic smf-lib style)
bus.subscribe(this); // this implements EventWatcher

// Subscribe with a lambda (Java 8 style)
bus.subscribe(event -> {
    if (AppEvent.DB_INSERT.equals(event)) refreshTable();
    if (AppEvent.DB_ERROR.equals(event))  showErrorBadge();
});

// Publish from anywhere
bus.publish(AppEvent.DB_INSERT);
bus.publish(AppEvent.APP_BUSY);

// Structured log
MessageLog log = MessageLog.getInstance();
log.info("Connection established");
log.warn("Retry attempt 2/3");
log.error("Insert failed: " + e.getMessage());
log.getByLevel(Level.ERROR).forEach(System.out::println);
```

---

### [04 — Loader & Application Bootstrap](04-loader-bootstrap/)
Initialize singletons in the right order before the main window appears.
Config from a `.properties` file, splash screen with real progress bar,
background loading with `SwingWorker`.

```java
Loader loader = Loader.getInstance();

loader.onProgress(pct -> progressBar.setValue(pct));
loader.onStep(result -> statusLabel.setText(result.getServiceName()));

if (loader.load()) {
    new MainWindow().setVisible(true);
}
```

Boot sequence:
```
MessageLog → AppEventBus → AppConfig (app.properties) → DataStore → DB check
```

`AppConfig` — external configuration, no recompile needed:
```properties
db.host=127.0.0.1
db.port=3306
db.name=myshop
db.user=root
db.password=
app.title=My Application
app.version=1.0
```

---

### [05 — Domain Model](05-domain-model/)
`Person → Customer / Employee / Supplier` interface hierarchy.
`AbstractPerson` implements all shared fields once — no duplication.
`ProductModel` and `BillModel` with line items and calculated totals.

```java
// Customer — all Person fields inherited from AbstractPerson
CustomerModel c = new CustomerModel();
c.setName("John");
c.setEmail("john@example.com");
c.setCity("Kandy");
c.setCountry("Sri Lanka");
c.save();   // INSERT → fires DB_INSERT event

CustomerModel loaded = CustomerModel.findById(1);
System.out.println(loaded.getDisplayName());
System.out.println(loaded.getFullAddress());

// Product
ProductModel p = new ProductModel();
p.setPurchasePrice(4.50);
p.setSalesPrice(6.80);
System.out.println(p.getMargin());    // 2.30
System.out.println(p.isLowStock());   // true/false

// Bill with line items
BillModel bill = new BillModel();
bill.setCustomerName("John");
bill.setTaxRate(0.08);
bill.addItem(1, "Arduino Nano", 3, 4.50);
bill.addItem(2, "ESP32 DevKit", 1, 6.80);
System.out.println(bill.getTotal());  // subtotal + tax + delivery
bill.save();
```

---

### [06 — Full Application](06-full-application/)
Everything wired into a complete working shop application.

Five tabs, each an independent `EventWatcher`:

| Tab | What it does |
|-----|-------------|
| 📊 Dashboard | Live counts, low stock alerts, real-time event log |
| 🛒 Sales | Browse products, build cart, checkout → saves invoice |
| 📦 Products | Add/delete products, live filter |
| 👥 Customers | Add/delete customers |
| 🧾 Bills | View all invoices, double-click for full itemized view |

None of the panels know about each other. They communicate only through
`AppEventBus` — when a sale is completed, `DataStore` fires an event,
the bus delivers it, and every subscribed panel refreshes itself.

```
SalesPanel.checkout()
    → BillModel.save()
        → DataStore.Insert()
            → AppEventBus.publish(DB_INSERT)
                → DashboardPanel.refresh()
                → ProductsPanel.refresh()
                → BillsPanel.refresh()
```

---

## Requirements

- **Java 8+**
- **MySQL** (optional — all demos run without a DB connection)
- `mysql-connector-java.jar` in classpath

---

## How to compile any tutorial

```bash
javac -cp lib/mysql-connector-java.jar -sourcepath src -d out src/demo/Example.java
java  -cp out:lib/mysql-connector-java.jar demo.Example
```

On Windows replace `:` with `;` in the classpath.

---

## Project structure

```
smf-tutorials/
├── 01-database-connection/    (8 Java files)
├── 02-uifill/                (13 Java files)
├── 03-event-message/         (11 Java files)
├── 04-loader-bootstrap/      (16 Java files)
├── 05-domain-model/          (20 Java files)
└── 06-full-application/      (35 Java files)
```

Each tutorial is self-contained with its own `src/`, `README.md`,
and runnable demo class.

---

## Key design patterns used

| Pattern | Where |
|---------|-------|
| Singleton | `DataStore`, `AppEventBus`, `MessageLog`, `AppConfig` |
| Interface + Implementation | `DatabaseConnection` / `DataStore`, `Person` / `AbstractPerson` |
| Observer | `EventWatcher` / `AppEventBus` |
| Template Method | `AbstractPerson` — shared fields, subclass-specific behaviour |
| DAO | `CustomerModel.findById()`, `ProductModel.findAll()` |

---

## What changed from the original (2012–2013)

| Original smf-lib | This series |
|---|---|
| Raw `HashSet`, `ArrayList` | Generics throughout |
| Anonymous inner class listeners | Java 8 lambdas |
| `GregorianCalendar` | Java 8 `LocalDate` / `LocalDateTime` |
| `getInstence()` (typo) | `getInstance()` |
| `MessegeCenter` (typo) | `MessageCenter` / `MessageLog` |
| Most domain methods `throw UnsupportedOperationException` | All methods implemented |
| `PropertyFile` interface (empty) | `AppConfig` — full .properties load/save |
| Hardcoded DB credentials | External config file |
| Loading on EDT | `SwingWorker` background loading |
| One giant `NewJFrame` class | Five focused, decoupled panels |
| `Billx` invoice number bug (string concat) | UUID-based invoice numbers |
| Person fields duplicated × 3 classes | `AbstractPerson` — declared once |

The architecture and design concepts are entirely from the original.

---

## Background

`smf-lib` was written between 2012 and 2013 by **stark** while learning Java,
inspired by seeing a Java-powered Mars rover. The library grew through 7 versions
(`v1-0` to `v1-6`), each one restructuring and improving the previous.

It was eventually used as the foundation for a shop application (`shade`),
which had tabbed panels for selling, customer management, and stock control —
the same structure as Tutorial 06, now completed.

The author went on to work in hardware design — Arduino, STM32, ESP32, AVR,
PCB design (KiCad, Altium, EasyEDA), firmware, and hardware reversing —
using Java for serial communication UIs and hardware simulators.
The discipline of the early smf-lib code carries through into that work.

---

## License

Original code © stark (2012–2013).
Modernized and documented 2025.
MIT License — use freely with attribution.
