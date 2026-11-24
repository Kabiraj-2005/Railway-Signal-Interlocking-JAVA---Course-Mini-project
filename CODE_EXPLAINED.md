# The Code, Explained Against Our Slides

This explains every file in the project and points to the slide that teaches each idea.
Slide references look like **D7 s53**, meaning *Deck 7, slide 53*.

| Code | Deck file in Downloads |
|------|------------------------|
| D2 | `2.Java Project Structure, packages and Classes.pptx` |
| D3 | `3.Method and Constructor Overloading.pptx` ("A Closer Look at Methods") |
| D4 | `4. Nested classes, Inheritance, Abstract Classes and Interfaces,.pptx` |
| D5 | `5. Java Coding Conventions and Build tools.pptx` |
| D6 | `6. Exception Handling.pptx` |
| D7 | `7. Collection Framework.pptx` (also covers Generics, `Objects`, `Optional`, `java.time`) |

Deck 1 (*Introduction to Java*, old `.ppt` format) could not be read reliably, so basics like
`if/else`, `for` loops, arrays and operators are not given slide numbers here.

> **Important:** decks 1–7 do **not** cover **JDBC, JavaFX, file I/O / serialization, logging or
> `Properties`**. If later decks exist, add their slide numbers. Otherwise, be ready to explain
> these parts in your own words. See [Not covered by decks 1–7](#not-covered-by-decks-17) at the end.

---

## 1. How the project is organised (D2, D5)

```
src/main/java/com/trains/interlocking/
├── model/      the things in a station (sections, points, signals, routes)
├── service/    the rules and actions
├── exception/  our three custom exceptions
├── dao/        database code
├── util/       settings, database connection, logging
└── ui/         the JavaFX window
src/main/resources/   app.properties and schema.sql
```

- **Maven folder layout**: `src/main/java` holds code and `src/main/resources` holds
  non-code files. **D2 s5, D5 s20**
- **Packages**: every file starts with `package com.trains.interlocking.model;` (or similar).
  Packages group related classes. **D2 s6, s11–12**
- **import** brings in classes from other packages: our own (`...model.Route`) and built-in
  ones (`java.util.ArrayList`, `java.sql.Connection`). **D2 s7–9**
- **pom.xml** lists the libraries (JavaFX, SQLite driver) and Maven downloads them.
  `mvn compile` builds the project. **D5 s19, s21–25**
- **Naming conventions**: classes in PascalCase (`TrackSection`), methods and variables in
  camelCase (`setRoute`, `trainPosition`), constants in UPPER_CASE (`NO_TRAIN`). **D5 s3–6**
- **Small methods with one job**, and comments that explain *why*. **D5 s10–11**

---

## 2. The model package (Utpal)

### TrackElement.java: the abstract parent

```java
public abstract class TrackElement implements Serializable {
    private final String id;
    private boolean locked;
    protected TrackElement(String id) { this(id, false); }
    protected TrackElement(String id, boolean locked) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.locked = locked;
    }
    public abstract String getStatus();
    public abstract String getDisplayColour();
```

| Idea in the code | What it means | Slides |
|------------------|---------------|--------|
| `abstract class`, `abstract` methods | You can't create a plain TrackElement; children must write `getStatus()` and `getDisplayColour()` | **D4 s26–29** |
| `private` fields + public getters | Encapsulation: data is hidden, accessed through methods | **D3 s17, s20** |
| `protected` constructors | Only subclasses (and the same package) can call them | **D3 s21, s24** |
| `final String id` | The id can never change after the object is made | **D3 s29** |
| Two constructors | Constructor overloading | **D3 s6–8** |
| `this.id = ...` | `this` means "the current object" | **D2 s24–25** |
| `this(id, false)` | One constructor calling the other one. *Not shown in the decks* (see the end) | — |
| `Objects.requireNonNull(id, ...)` | Stops a `null` id straight away with a clear message | **D7 s124, s131–132** |
| `implements Serializable` | Lets the object be saved to a snapshot file. *Not in the decks* | — |

**Why no `equals()`/`hashCode()`?** Every part of the program uses the *same* objects from one
layout map, so the default "is it the same object?" check is correct. (Comparing objects of the
same class is shown on **D2 s26–27**.)

### TrackSection.java, Point.java, Signal.java: the three children

```java
public class TrackSection extends TrackElement {
    private boolean occupied;
    public TrackSection(String id) { super(id); }
    public TrackSection(String id, boolean occupied, boolean locked) {
        super(id, locked);
        this.occupied = occupied;
    }
    @Override
    public String getStatus() { ... }
```

| Idea | Slides |
|------|--------|
| `extends TrackElement` (inheritance) | **D4 s9–10** |
| Private parent fields are reached through the parent's public methods (`isLocked()`) | **D4 s11** |
| `super(id)` calls the parent constructor | **D4 s16, s18–19** |
| Parent constructor runs first | **D4 s12** |
| `@Override getStatus()` (method overriding) | **D4 s22–25** |
| Overloaded constructors (short and full) | **D3 s6–8** |

**Point.setReversed** throws our checked exception:

```java
public void setReversed(boolean newPosition) throws PointLockedException {
    if (newPosition != reversed && isLocked()) {
        throw new PointLockedException("Point " + getId() + " is locked and cannot move");
    }
    reversed = newPosition;
}
```
`throws` in the header and `throw new ...` in the body: **D6 s25–28**.

**`Point.positionName(boolean)`** is a `static` method: it uses no object's fields, so you
call it on the class (`Point.positionName(true)`). **D3 s26–28**

**Polymorphism in action:** the UI holds every element as a `TrackElement` and calls
`element.getStatus()`. Java runs the `TrackSection`, `Point` or `Signal` version depending on
the real object. A parent-type variable can point to a child object: **D4 s14**, overriding: **D4 s22–23**.

### Route.java

```java
public static final int NO_TRAIN = -1;
private final ArrayList<TrackSection> sections;
private final HashMap<Point, Boolean> pointPositions; // true = REVERSE
```

| Idea | Slides |
|------|--------|
| `static final` constant `NO_TRAIN` | **D3 s26, s29** |
| `ArrayList<TrackSection>` in travel order | **D7 s6–9** |
| `sections.get(i)`, `sections.size()` | **D7 s12, s36** |
| `HashMap<Point, Boolean>`: point → position needed | **D7 s59–61** |
| `pointPositions.containsKey(point)` in `usesPoint` | **D7 s63** |
| `pointPositions.keySet()` in the for-each loops | **D7 s66, s77** |
| `other.getSections().contains(section)` in `conflictWith` | **D7 s17, s36** |
| Generics `<TrackSection>`, `<Point, Boolean>`: type-safe, no casting | **D7 s91–100** |
| `toString()` override (used by the route list on screen) | **D7 s89–90, s130** |

**`conflictWith(other)`, step by step:**
1. Loop over my sections; if the other route `contains` one → return "both use section …".
2. Loop over my points (`keySet()`); if the other route uses the same point (`containsKey`)
   but needs a different position (`get` gives the two Booleans) → return "… different positions".
3. Otherwise return `null`, meaning no conflict.

### EventLogEntry.java

A simple class with four `final` fields, a constructor and getters. The time is a
`LocalDateTime`: **D7 s161** (LocalDate **s158**, LocalTime **s160**). `toString()` is overridden
so the log list can show each entry as one line.

---

## 3. The exception package (Utpal)

```java
public class RouteConflictException extends Exception {
    private static final long serialVersionUID = 1L;
    public RouteConflictException(String message) { super(message); }
}
```

| Idea | Slides |
|------|--------|
| Checked vs unchecked: extending `Exception` makes it **checked** | **D6 s3, s16** |
| Exceptions are objects; the `Throwable` hierarchy | **D6 s14–15** |
| Creating your own exception: extend `Exception`, give it a String constructor | **D6 s29–34** |
| `super(message)` passes the message to the parent; `getMessage()` reads it back | **D6 s29, s32** |

Where each one is thrown:
- `RouteConflictException`: route already set, conflicts with an active route, section
  locked, cancel/move not allowed, unknown route.
- `SectionOccupiedException`: a section on the route is occupied, or you try to mark a locked section.
- `PointLockedException`: point locked, or a train is over it.

---

## 4. The service package (Utpal)

### StationStore.java: an interface

```java
public interface StationStore {
    void saveState(List<TrackElement> elements, List<Route> routes) throws SQLException;
    void logEvent(EventLogEntry entry) throws SQLException;
}
```
An interface lists *what* must be done, not *how*. `StationRepository` (dao) implements it, so
the service never sees SQL. **D4 s31–32, s45**

### StationFactory.java

- `static` methods that build the station, so no object is needed: **D3 s26–28**.
- `layout.put("T1", new TrackSection("T1"))`: **D7 s60**.
- `(TrackSection) layout.get("T1")` is a cast. The map stores `TrackElement`, and we know "T1"
  is a `TrackSection`. Casting is discussed on **D7 s94, s97**.
- **Why take objects *from* the layout?** So a route and the screen share the same T1 object.
  Locking it through the route shows on screen. (Object references: **D2 s17**.)

### SafetyRules.java: checks only, never changes anything

**`checkRouteCanBeSet(route, allRoutes)`** runs these checks in order, and the first failure throws:
1. Route already active → `RouteConflictException`
2. Any section occupied → `SectionOccupiedException`
3. Conflicts with an active route (`conflictWith`) → `RouteConflictException`
4. Any section locked → `RouteConflictException`
5. Any point that must move → `checkPointCanMove`

**`sectionsAtPoint(point, allRoutes)`** finds the section a point lies in:
```java
common = new HashSet<>(route.getSections());   // first route: all its sections
common.retainAll(route.getSections());         // later routes: keep only the shared ones
```
P1: {T1, T2} ∩ {T1, T3} = **{T1}**. `HashSet` **D7 s43–45**, `retainAll` (intersection)
**D7 s53** (on lists: **D7 s25**).

Exception slides: `throws` with several exceptions **D6 s25–26**; the exception travels up to
whoever called the method **D6 s21–23**.

### InterlockingService.java: the actions

**`setRoute(routeId)`**
1. `findRoute(routeId)`: a loop over the list, comparing ids with `equals`.
2. `try { SafetyRules.checkRouteCanBeSet(...) }` with **three catch blocks**, one per
   exception (**D6 s8, s18**). Each catch writes "REFUSED" to the log and then does `throw e;`,
   passing the same exception on to the UI. (Exception propagation: **D6 s21–24**.)
3. Move and lock each point (`keySet()` loop), lock each section, set `active`, turn the signal GREEN.
4. `saveAndRecord(...)`: save to the database, then add to the log.

**`moveTrain(routeId)`**: one call moves the train one section.
- `trainPosition == NO_TRAIN` → the train enters: signal RED, first section occupied, position 0.
- Otherwise the section being left becomes clear and is **released**. If there is a next section
  it becomes occupied; if not, the route ends and its points are released.

**`cancelRoute`** refuses if the route isn't set or a train is on it. Otherwise it turns the
signal RED and releases everything.

**`saveAndRecord` / `addToLog`**: if the database save fails, the `SQLException` is
**caught and logged** so the program keeps working (**D6 s17, s13**). Each event is added to an
`ArrayList` (**D7 s10**).

**Getters return copies**, `new ArrayList<>(routes)`, so other classes cannot add or remove
our items. (Passing objects by reference, which is why copies matter: **D3 s13–14**.)

**`addHistory(...)`** uses `events.addAll(0, history)` to put old events first. **D7 s23**

### SnapshotFile.java

`ObjectOutputStream.writeObject` and `ObjectInputStream.readObject` save and restore objects.
It writes a count, then each object; reading does the same in the same order. The streams are
opened in `try ( ... )` (try-with-resources), which closes them automatically.
*Serialization, object streams and try-with-resources are not in decks 1–7.* The casts
`(TrackElement) in.readObject()` relate to **D7 s94, s97**.

---

## 5. util/LogConfig.java (Utpal)

A `static` field and a `static` method (**D3 s26–28**). It gets a `Logger`, sets the level, adds
a `FileHandler` with a `SimpleFormatter`, and uses a `boolean configured` flag so the handler is
only added once. *Logging is not in decks 1–7.* The `try/catch (IOException e)` follows **D6 s17**.

---

## 6. Main.java: the test harness (Utpal)

- `public static void main(String[] args)` is the entry point. **D4 s6–7** (command-line arguments).
- 15 `private static` test methods, one per scenario. **D3 s26**, **D5 s11** (small methods).
- **Pattern for a "should be refused" test** (**D6 s17–18**):
  ```java
  try {
      service.setRoute("R1");
      service.throwPoint("P1");          // should throw
      report("11. ...", false);           // only reached if nothing was thrown → FAIL
  } catch (PointLockedException e) {
      report("11. ...", true);            // the right exception → PASS
  } catch (Exception e) {
      report("11. ... (" + e.getMessage() + ")", false);  // a wrong exception → FAIL
  }
  ```
  The specific catch comes **before** `catch (Exception e)`, because catch blocks are checked
  from top to bottom. **D6 s9, s18**
- `static int passed`, `total` are counters shared by all test methods. **D3 s26**

---

## 7. The dao package and database utilities (Harsh)

*JDBC, SQL and `Properties` are not in decks 1–7.* The Java ideas around them are:

| Code | Java idea | Slides |
|------|-----------|--------|
| `public interface Dao<T>` | Generic interface: `T` becomes `TrackSection`, `Point`, ... | **D7 s91–101**, **D4 s31** |
| `class PointDao implements Dao<Point>` | Implementing an interface | **D4 s32** |
| `Optional<T> findById(...)`, `Optional.of(...)`, `Optional.empty()`, `.get()` | A value that may be missing | **D7 s134–140** |
| `List<T> findAll()` returning an `ArrayList` | List interface / ArrayList | **D7 s6–7** |
| `throws SQLException` | `SQLException` is a checked exception, as the deck itself says | **D6 s3** |
| `StationRepository implements StationStore` | Interface implemented by another package | **D4 s31–32** |
| `instanceof` to pick the right DAO | Checking an object's real class | **D6 s37** (used in the example) |
| `EventLogDao`: `LinkedList` + `addFirst()` | Adds at the front, which reverses newest-first rows | **D7 s28–31** |
| `RouteDao`: `HashMap` lookups + casts | Turn ids into the layout's own objects | **D7 s61**, **D7 s94–97** |

**The transaction in `StationRepository.saveState`** (all rows saved, or none):
```java
conn.setAutoCommit(false);      // start
try {
    ... update every element and route ...
    conn.commit();              // success: make it permanent
} catch (SQLException e) {
    conn.rollback();            // failure: undo everything
    throw e;
} finally {
    conn.setAutoCommit(true);   // always runs
}
```
The JDBC calls are outside the decks, but the **try / catch / finally** shape is **D6 s19–20**.

**AppConfig**: the `static { ... }` block runs once when the class is first used. **D3 s27**

---

## 8. The ui package (Aryan)

*JavaFX is not in decks 1–7.* The Java ideas inside it:

| Code | Idea | Slides |
|------|------|--------|
| `MainApp extends Application`, overriding `init()`, `start()`, `stop()` | Inheritance + overriding | **D4 s9, s22** |
| `private static final String[] TRACK_ORDER = {...}` | Constant array | **D3 s29** (final), Deck 1 (arrays) |
| `HashMap<String, Label> trackLabels` | id → label on screen | **D7 s59–61** |
| `for (Route route : service.getRoutes())` | for-each over a list | **D7 s41** |
| `button.setOnAction(e -> onSetRoute());` | A lambda as the click handler (lambdas only appear in the Comparator slides) | **D7 s82, s90** |
| Each handler: `try { ... } catch (Exception e) { showError(...) }` | Catching exceptions, then `getMessage()` | **D6 s17, s32** |
| `element.getDisplayColour()` with no `instanceof` | Polymorphism | **D4 s14, s22–23** |
| `Launcher` is a separate class with `main` | Entry point | **D4 s6** |

---

## Not covered by decks 1–7

Be ready to explain these in your own words, or find them in later decks:

| Construct | Where in the code | Short explanation to give |
|-----------|-------------------|---------------------------|
| **JDBC** (`DriverManager`, `Connection`, `PreparedStatement`, `ResultSet`, transactions) | dao, DatabaseConnection | How Java talks to a database; `?` placeholders keep data separate from SQL |
| **JavaFX** | MainApp, Launcher | The GUI library; `init → start → stop` lifecycle |
| **Serialization**, `ObjectOutputStream` / `ObjectInputStream`, `Serializable`, `serialVersionUID` | SnapshotFile, model | Turning objects into bytes in a file and back |
| **File I/O** (`BufferedReader`, `FileInputStream`, `getResourceAsStream`) | DatabaseConnection, AppConfig, SnapshotFile | Reading files line by line or as bytes |
| **Logging** (`Logger`, `FileHandler`, `SimpleFormatter`, `Level`) | LogConfig, InterlockingService | Writing messages to `interlocking.log` |
| **Properties** | AppConfig | Reads `key=value` settings files |
| **try-with-resources** `try (X x = ...) { }` | every DAO, SnapshotFile, AppConfig | Closes the resource automatically, like a built-in `finally` |
| **`this(...)`** constructor call | TrackElement | One constructor calling another in the same class (the decks show `this.x` and `super(...)` only) |
| **`throw e;`** (rethrowing a caught exception) | InterlockingService, StationRepository, DatabaseConnection | After doing something in `catch`, pass the same exception on to the caller |
| **Ternary** `a ? b : c` | DAOs (`flag ? 1 : 0`) | A one-line if/else (probably in Deck 1, which could not be read) |
| **`StringBuilder`** | DatabaseConnection.readSchema | Builds a long String piece by piece efficiently |
| **`MainApp.class`** | Launcher | An object that describes a class; JavaFX uses it to create the app |
