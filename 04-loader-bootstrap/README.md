# SMF Tutorial 04 — Loader & Application Bootstrap

## What this covers

How to wire everything from Tutorials 01–03 into a clean startup sequence:
- Load config from a file before connecting to the database
- Initialize singletons in the correct order
- Show a splash screen with real progress during boot
- Handle boot failures gracefully
- Launch the main window only when everything is ready

Also covers the completed `AppUtils` (date/time, image loading, button busy state)
and `AppConfig` (the `PropertyFile` interface that was left empty in the original).

Based on the original `smf-lib` `Loader` / `Loaderx` by **stark**, modernized for **Java 8**.

---

## Files

```
src/
├── smf/
│   ├── loader/
│   │   ├── AppLoader.java      ← interface: bootstrap contract
│   │   ├── AppConfig.java      ← loads/saves app.properties (was PropertyFile)
│   │   ├── LoadResult.java     ← typed step result (replaces raw HashSet errors)
│   │   └── Loader.java         ← implementation: full boot sequence
│   ├── util/
│   │   └── AppUtils.java       ← date/time, image load, button busy state
│   ├── database/               ← from Tutorial 01
│   ├── events/                 ← from Tutorials 01 & 03
│   └── messages/               ← from Tutorials 01 & 03
└── demo/
    └── BootstrapDemo.java      ← SplashScreen + MainWindow, full runnable demo
```

---

## How to compile and run

```bash
javac -cp lib/mysql-connector-java.jar -sourcepath src -d out src/demo/BootstrapDemo.java
java -cp out:lib/mysql-connector-java.jar demo.BootstrapDemo
```

On Windows use `;` instead of `:`.

The demo runs without a database — DB connection check is an optional step,
so boot succeeds even if MySQL isn't running.

---

## Boot sequence

```
main()
  └── SplashScreen shown
        └── SwingWorker (background thread)
              └── Loader.load()
                    ├── Step 1: MessageLog       → singleton initialized
                    ├── Step 2: AppEventBus      → singleton initialized
                    ├── Step 3: AppConfig        → app.properties loaded
                    ├── Step 4: DataStore        → configured from config
                    │                            → wired into AppEventBus
                    └── Step 5: DB reachable?    → optional check
              └── All required steps OK?
                    ├── YES → SplashScreen.dispose() → MainWindow.show()
                    └── NO  → Error dialog with LoadResult list → System.exit(1)
```

---

## Key concepts

### 1. Boot order matters

Singletons must start in the right order:
1. `MessageLog` first — everything else logs to it
2. `AppEventBus` before any subscribers register
3. `AppConfig` before `DataStore` — config provides the DB settings
4. `DataStore` last — needs config to know where to connect

The original smf-lib used `Class.forName()` reflection:
```java
// Original — fragile, no compile-time checking
Class.forName("base.MessegeCenter.SystemMesseges").newInstance();
```

Modernized — direct singleton calls, type-safe:
```java
MessageLog.getInstance();
AppEventBus.getInstance();
AppConfig.getInstance().load();
```

---

### 2. Config file — app.properties

On first run, default `app.properties` is created automatically:

```properties
db.host=127.0.0.1
db.port=3306
db.name=mydb
db.user=root
db.password=
db.timeout=5000
app.title=SMF Application
app.version=1.0
```

Change the file to point at your database — no recompile needed.

Reading config in your code:
```java
AppConfig config = AppConfig.getInstance();
String host    = config.get("db.host", "127.0.0.1");
int    timeout = config.getInt("db.timeout", 5000);
```

---

### 3. Progress callbacks

```java
Loader loader = Loader.getInstance();

loader.onProgress(pct -> progressBar.setValue(pct));

loader.onStep(result -> {
    statusLabel.setText("Loading: " + result.getServiceName());
    if (result.isFailed()) showErrorIcon();
});

boolean ok = loader.load();
```

---

### 4. SwingWorker — keep UI responsive

Never run Loader on the EDT (Event Dispatch Thread). Use SwingWorker:

```java
SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
    @Override
    protected Boolean doInBackground() {
        return Loader.getInstance().load();  // runs on background thread
    }

    @Override
    protected void done() {
        // runs back on EDT when doInBackground() finishes
        if (get()) showMainWindow();
        else       showErrorDialog();
    }
};
worker.execute();
```

---

### 5. Adding your own boot steps

```java
// In Loader.java, add inside load():
totalSteps = 6;  // increment this

step("MyService", true, () -> {
    MyService.getInstance().initialize();
});

stepOptional("Cache Warm-up", () -> {
    Cache.getInstance().preload();
});
```

---

### 6. AppUtils — date/time (replaces GregorianCalendar)

```java
// Original smf-lib used GregorianCalendar — verbose and awkward
GregorianCalendar cal = new GregorianCalendar();
String date = extrax.gregorian_to_string(cal);

// Modernized with Java 8 LocalDate
String date     = AppUtils.getDate();          // "2013-07-05"
String time     = AppUtils.getTime();          // "14:23:01"
String both     = AppUtils.getDateAndTime();   // "2013-07-05 14:23:01"

LocalDate d     = AppUtils.stringToDate("2013-07-05");
String str      = AppUtils.dateToString(d);
```

---

### 7. Button busy state (replaces TaskButtonx)

```java
// Put button in busy state while doing work
Object[] state = AppUtils.buttonBusy(saveButton);

// ... do the work (DB save, etc.) ...

// Restore when done
AppUtils.buttonRestore(saveButton, state);
```

---

## What changed from the original smf-lib

| Original | Modernized |
|---|---|
| `Class.forName()` reflection to load services | Direct singleton calls — type-safe |
| `HashSet errors` (raw, unordered) | `List<LoadResult>` — typed, ordered, labeled |
| `Loaderx` interface (Load, getErrors, isLoaded) | `AppLoader` + progress/step callbacks |
| `PropertyFile` interface (empty — never implemented) | `AppConfig` — full .properties load/save |
| `Extrax` — most methods threw `UnsupportedOperationException` | `AppUtils` — all methods implemented |
| `GregorianCalendar` for dates | Java 8 `LocalDate` / `LocalDateTime` |
| `TaskButtonx.install()` / `Animate()` / `Restore()` | `AppUtils.buttonBusy()` / `buttonRestore()` |
| `new NewJFrame().setVisible(true)` direct in Loader | Callback-driven — Loader doesn't know about the UI |
| Loading on EDT — could freeze the window | `SwingWorker` — background thread, responsive UI |

---

## Tutorials in this series

| # | Topic |
|---|---|
| 01 | Database Connection — DataStore, interfaces, singleton |
| 02 | UIFill — JTable, JComboBox, JList from ResultSet |
| 03 | EventWatcher & MessageCenter — observer pattern |
| **04** | **Loader & Bootstrap — startup sequence, config, splash screen** |
| 05 | Domain Model — Person, Customer, Supplier structures |
