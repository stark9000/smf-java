# SMF Tutorial 03 — EventWatcher & MessageCenter

## What this covers

The Observer pattern in practice — how to decouple application components
so they communicate through events rather than direct method calls.

Also covers the enhanced `MessageLog` — a replacement for the original
`SystemMessages` with severity levels, timestamps, and ordered storage.

Based on the original `smf-lib` EventWatcher / MessegeCenter layer by **stark**,
modernized and extended for **Java 8**.

---

## Files

```
src/
├── smf/
│   ├── events/
│   │   ├── EventWatcher.java     ← interface: receives events (original)
│   │   ├── Event.java            ← interface: fires events (original)
│   │   ├── AppEventBus.java      ← singleton bus: many-to-many event routing
│   │   └── AppEvent.java         ← typed event constants (replaces raw strings)
│   ├── messages/
│   │   ├── MessageCenter.java    ← interface (original, from Tutorial 01)
│   │   ├── SystemMessages.java   ← original implementation (from Tutorial 01)
│   │   └── MessageLog.java       ← enhanced: levels + timestamps + ordering
│   └── database/                 ← from Tutorial 01
└── demo/
    └── EventWatcherDemo.java     ← runnable Swing demo (3 panels, 1 bus)
```

---

## How to compile and run

```bash
javac -sourcepath src -d out src/demo/EventWatcherDemo.java
java -cp out demo.EventWatcherDemo
```

No database required — this tutorial is self-contained.

---

## Key concepts

### 1. The original pattern (smf-lib style)

In the original smf-lib, `NewJFrame` implemented `EventWatcher` directly:

```java
public class NewJFrame extends JFrame implements EventWatcher {

    public NewJFrame() {
        datastore.registerWatcher(this);   // subscribe
    }

    @Override
    public void update(Object event) {
        System.out.println("DB event fired: " + event);
        // React to "--insert", "--select", etc.
    }
}
```

This works well for one listener. Tutorial 03 extends it to support
many listeners and many publishers through `AppEventBus`.

---

### 2. AppEventBus — many-to-many

```
DataStore ──┐
SerialPort ─┼──► AppEventBus ──► StatusPanel (implements EventWatcher)
TimerTask  ─┘                ──► LogPanel    (lambda subscriber)
                             ──► CartPanel   (lambda subscriber)
```

Any component can publish. Any component can subscribe. None of them
need a direct reference to each other.

---

### 3. Subscribing — two ways

**Classic style** (implements EventWatcher):
```java
public class MyPanel extends JPanel implements EventWatcher {

    MyPanel(AppEventBus bus) {
        bus.subscribe(this);
    }

    @Override
    public void update(Object event) {
        if (AppEvent.DB_INSERT.equals(event)) {
            refreshTable();
        }
    }
}
```

**Lambda style** (Java 8 — no interface needed):
```java
bus.subscribe(event -> {
    if (AppEvent.DB_ERROR.equals(event)) {
        showErrorDialog();
    }
});
```

---

### 4. Publishing events

```java
AppEventBus bus = AppEventBus.getInstance();

bus.publish(AppEvent.DB_INSERT);     // after a successful insert
bus.publish(AppEvent.APP_BUSY);      // before a slow operation
bus.publish(AppEvent.APP_READY);     // when it finishes
bus.publish(AppEvent.DATA_CHANGED);  // when any data is modified
```

---

### 5. Typed event constants

Instead of raw strings (fragile, typos possible):
```java
notifyWatchers(" --insert");   // original smf-lib
```

Use typed constants (compile-time checked):
```java
bus.publish(AppEvent.DB_INSERT);   // modernized
```

Add your own by extending AppEvent:
```java
public class ShopEvent extends AppEvent {
    public static final String CART_UPDATED = "cart-updated";
    public static final String ORDER_PLACED = "order-placed";
}
```

---

### 6. Unsubscribing — prevent memory leaks

Always unsubscribe when a panel or dialog is closed:

```java
// Classic style
bus.unsubscribe(this);

// Lambda style — save the returned reference
EventWatcher w = bus.subscribe(event -> doSomething());
// later:
bus.unsubscribe(w);
```

---

### 7. MessageLog — enhanced SystemMessages

```java
MessageLog log = MessageLog.getInstance();

log.info("Connection established");
log.warn("Retry attempt 2 of 3");
log.error("Insert failed: " + e.getMessage());

// Get all messages
log.getAll().forEach(System.out::println);

// Get only errors
log.getByLevel(Level.ERROR).forEach(System.out::println);

// Check if anything went wrong
if (log.hasErrors()) showErrorBadge();
```

Output format:
```
[14:23:01] [INFO]  Connection established
[14:23:05] [WARN]  Retry attempt 2 of 3
[14:23:07] [ERROR] Insert failed: Duplicate entry
```

---

## What changed from the original smf-lib

| Original | Modernized |
|---|---|
| `EventWatcher` on `NewJFrame` only | `AppEventBus` — any class can subscribe |
| Only DataStore fires events | Any component can publish |
| Raw string events `"--insert"` | Typed constants `AppEvent.DB_INSERT` |
| `MessegeCenter` (typo, raw HashSet) | `MessageLog` with Level, timestamp, ordering |
| No unsubscribe pattern | `unsubscribe()` to prevent memory leaks |
| Anonymous inner class listeners | Java 8 lambda subscriptions |

The core idea — components publishing events and listeners reacting —
is 100% from the original smf-lib design.

---

## Next tutorial

**04 — Loader & Application Bootstrap**
(How to wire DataStore + MessageLog + EventBus + UI into a clean startup sequence)
