# SMF Tutorial 01 — Database Connection

## What this covers

How to connect a Java application to MySQL using the SMF `DataStore` —
a singleton that handles connection, CRUD operations, and event notifications.

This is based on the original `smf-lib` framework written by **stark** while learning Java,
modernized for **Java 8**.

---

## Files

```
src/
├── smf/
│   ├── database/
│   │   ├── DatabaseConnection.java   ← interface: connection contract
│   │   ├── DatabaseFunctions.java    ← interface: CRUD contract
│   │   └── DataStore.java            ← implementation: singleton DB layer
│   ├── events/
│   │   ├── Event.java                ← interface: fires events to watchers
│   │   └── EventWatcher.java         ← interface: receives events
│   └── messages/
│       ├── MessageCenter.java        ← interface: message bus contract
│       └── SystemMessages.java       ← implementation: singleton message log
└── Example.java                      ← runnable usage example
```

---

## Requirements

- Java 8+
- MySQL running (local or remote)
- `mysql-connector-java.jar` in classpath

---

## Setup

Create the test database and table:

```sql
CREATE DATABASE smf_test;
USE smf_test;
CREATE TABLE products (
    id    INT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(100),
    price DECIMAL(10,2)
);
```

---

## How to compile and run

```bash
# Compile (with MySQL connector in lib/)
javac -cp lib/mysql-connector-java.jar -sourcepath src -d out src/Example.java

# Run
java -cp out:lib/mysql-connector-java.jar Example
```

On Windows use `;` instead of `:` in the classpath.

---

## Key concepts

### Singleton pattern
`DataStore` and `SystemMessages` are singletons — one instance shared across
the whole application. Get them anywhere with:

```java
DataStore db = DataStore.getInstance();
SystemMessages log = SystemMessages.getInstance();
```

### Programming to interfaces
The actual implementation is `DataStore`, but it is referenced through
`DatabaseConnection` and `DatabaseFunctions` interfaces. This means you can
swap MySQL for another database engine without changing any calling code.

```java
DatabaseFunctions db = DataStore.getInstance();
db.Insert(...);
db.Select(...);
```

### Event watching
Register a listener to react to any DB operation:

```java
db.registerWatcher(event -> {
    if (event.equals("--insert")) {
        System.out.println("Something was inserted!");
    }
});
```

Events fired: `--insert`, `--select`, `--update`, `--delete`,
and `-error` variants for failures.

### System messages
All errors and status messages are collected in `SystemMessages`.
Read them any time:

```java
SystemMessages.getInstance().getMessages().forEach(System.out::println);
```

---

## What changed from the original smf-lib

| Original | Modernized |
|----------|------------|
| `MessegeCenter` (typo) | `MessageCenter` |
| Raw `HashSet` | `HashSet<String>` / `Set<String>` |
| Anonymous inner classes | Java 8 lambdas |
| Manual resource closing | `try-with-resources` |
| `instence` (typo) | `instance` |
| `remveWatcher` (typo) | `removeWatcher` |
| `setSeverIP` (typo) | `setServerIP` |

The architecture and design concepts are unchanged — just cleaned up.

---

## Next tutorial

**02 — UIfill: Populating Swing components from database results**
(`jTableFill`, `jComboBoxFill`, `jListFill`)
