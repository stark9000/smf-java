# SMF Tutorial 05 — Domain Model

## What this covers

The domain model hierarchy: `Person → Customer / Employee / Supplier`,
plus `ProductModel` and `BillModel` — each able to save and load
themselves from the database.

The key design lesson: **define fields once, inherit everywhere**.
Based on the original `smf-lib` structures by **stark**, fully completed
for Java 8.

---

## Files

```
src/
├── smf/
│   ├── model/
│   │   ├── structures/
│   │   │   ├── Person.java       ← interface: 30+ field contract
│   │   │   ├── Customer.java     ← interface: extends Person
│   │   │   └── Employee.java     ← interface: extends Person
│   │   ├── AbstractPerson.java   ← implements Person once for all
│   │   ├── CustomerModel.java    ← Customer + DB save/load/find
│   │   ├── EmployeeModel.java    ← Employee + DB save
│   │   ├── ProductModel.java     ← standalone product + DB
│   │   └── BillModel.java        ← invoice with line items + totals
│   ├── util/
│   │   └── AppUtils.java
│   └── database / events / messages  ← from previous tutorials
└── demo/
    └── DomainModelDemo.java      ← 3-panel Swing demo, no DB required
```

---

## How to compile and run

```bash
javac -cp lib/mysql-connector-java.jar -sourcepath src -d out src/demo/DomainModelDemo.java
java -cp out:lib/mysql-connector-java.jar demo.DomainModelDemo
```

The demo runs without a database. Save buttons will log errors
gracefully if MySQL is not running.

---

## Database setup

```sql
CREATE DATABASE smf_test;
USE smf_test;

CREATE TABLE customers (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100),
    full_name    VARCHAR(150),
    email        VARCHAR(100),
    phone        VARCHAR(30),
    city         VARCHAR(80),
    country      VARCHAR(80),
    date_joined  DATE,
    active       TINYINT DEFAULT 1,
    note         TEXT
);

CREATE TABLE employees (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    employee_id   VARCHAR(30),
    name          VARCHAR(100),
    email         VARCHAR(100),
    salary_type   VARCHAR(20),
    salary        DECIMAL(10,2),
    join_date     DATE,
    employee_type VARCHAR(20)
);

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

CREATE TABLE bills (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    invoice_number  VARCHAR(30) UNIQUE,
    customer_id     INT,
    customer_name   VARCHAR(100),
    payment_date    DATE,
    currency        VARCHAR(10) DEFAULT 'USD',
    delivery_charge DECIMAL(10,2) DEFAULT 0,
    tax_rate        DECIMAL(5,4) DEFAULT 0,
    total           DECIMAL(10,2),
    status          VARCHAR(20) DEFAULT 'PENDING'
);

CREATE TABLE bill_items (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    invoice_number  VARCHAR(30),
    product_id      INT,
    product_name    VARCHAR(100),
    quantity        INT,
    unit_price      DECIMAL(10,2),
    line_total      DECIMAL(10,2)
);
```

---

## Key concepts

### 1. The hierarchy

```
Person (interface)
  ↓ implements
AbstractPerson (abstract class — all 30+ fields, once)
  ↓ extends
CustomerModel   (adds customer-specific fields + DB save/load)
EmployeeModel   (adds employee-specific fields + DB save)

Standalone (not Person):
ProductModel    (product fields + DB save/load)
BillModel       (invoice + line items + calculated totals)
```

---

### 2. AbstractPerson eliminates duplication

In the original smf-lib, `Customerx`, `Employeex`, and `Supplierx`
each had their own copy of all 30 Person fields. Any change to
a Person field had to be made in three places.

```java
// Original — 30 fields × 3 classes = 90 field declarations
public class Customerx implements Person, Customer {
    private String Name = "";
    private String FullName = "";
    // ... 28 more ...
}

public class Employeex implements Person, Employee {
    private String Name = "";   // duplicate!
    private String FullName = ""; // duplicate!
    // ... 28 more ...
}
```

With `AbstractPerson`:
```java
// Modernized — 30 fields declared once
public abstract class AbstractPerson implements Person { ... }

public class CustomerModel extends AbstractPerson implements Customer {
    // Only Customer-specific fields here
}

public class EmployeeModel extends AbstractPerson implements Employee {
    // Only Employee-specific fields here
}
```

---

### 3. Creating and saving a customer

```java
CustomerModel c = new CustomerModel();
c.setName("John");
c.setEmail("john@example.com");
c.setCity("Kandy");
c.setCountry("Sri Lanka");

// Convenience methods from AbstractPerson
System.out.println(c.getDisplayName()); // "John"
System.out.println(c.getFullAddress()); // "Kandy, Sri Lanka"

// Save — fires DB_INSERT event through AppEventBus
c.save();

// Load back
CustomerModel loaded = CustomerModel.findById(1);
```

---

### 4. Building a Bill with line items

```java
BillModel bill = new BillModel();
bill.setCustomerName("John");
bill.setCurrency("USD");
bill.setDeliveryCharge(2.50);
bill.setTaxRate(0.08);  // 8%

bill.addItem(1, "Arduino Nano",  3, 4.50);
bill.addItem(2, "ESP32 DevKit",  1, 6.80);

System.out.println(bill.getSubTotal());   // 20.30
System.out.println(bill.getTaxAmount());  // 1.624
System.out.println(bill.getTotal());      // 24.424

bill.save();  // saves header + all line items
```

---

### 5. ProductModel calculated fields

```java
ProductModel p = new ProductModel();
p.setPurchasePrice(4.50);
p.setSalesPrice(6.80);
p.setCurrentStock(3);
p.setMinStock(10);

System.out.println(p.getMargin());    // 2.30
System.out.println(p.isLowStock());   // true
```

---

## What changed from the original smf-lib

| Original | Modernized |
|---|---|
| `Customerx` — Customer methods threw `UnsupportedOperationException` | `CustomerModel` — all methods implemented |
| `Employeex` — Employee methods threw `UnsupportedOperationException` | `EmployeeModel` — all methods implemented |
| `Billx` — all Bill methods threw `UnsupportedOperationException` | `BillModel` — full invoice with line items |
| `Billx.setInvoiceNumber()` — concatenated "1" to last ID (bug) | UUID prefix — guaranteed unique |
| Person fields duplicated in Customerx, Employeex, Supplierx | `AbstractPerson` — declared once |
| `GregorianCalendar` in Employee dates | Java 8 `LocalDate` |
| No `findById()`, `findAll()` | Static finder methods on every model |
| No line items in Bill | `BillModel.BillItem` with quantity × price |

---

## Next tutorial

**Tutorial 06 — Full Application**
Everything from 01–05 wired into one complete working shop application.
