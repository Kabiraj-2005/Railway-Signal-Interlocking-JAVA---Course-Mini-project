# Study Guide: Railway Signal Interlocking (UCS553)

One section per team member. For each file you own you will find:

- **What it does**: plain English
- **How the key methods work**: step by step
- **Lecture topics**: which deck topic it shows. *We did not have the deck files, so slide
  numbers are left blank: write them in as `(slides __)` from your own decks.*
- **Viva questions**: five likely questions with short model answers

Files that are almost the same (the three exceptions, and the three simple DAOs) share one section.

## The big picture (everyone should know this)

```
              T3 (loop platform)
          /----------------------\
 T1 ---- P1 ------ T2 (main) ----- P2 ---- T4
 [SIG_W]                                [SIG_E]
```

- **Section** (T1–T4): a piece of track. It can be *occupied* (a train is on it) and *locked*
  (a route has reserved it).
- **Point** (P1, P2): moving rails. NORMAL sends the train to the main line (T2), REVERSE to the loop (T3).
- **Signal** (SIG_W, SIG_E): RED = stop, GREEN = go.
- **Route** (R1–R4): signal + sections + required point positions.
  R1 West→Main, R2 West→Loop, R3 East→Main, R4 East→Loop.

How the layers talk to each other:

```
 MainApp (ui, Aryan)  ──calls──►  InterlockingService (service, Utpal)
                                     │  asks SafetyRules "is this safe?"
                                     │  changes model objects (Route, Point, ...)
                                     ▼
                                  StationStore (interface)
                                     ▲ implemented by
                                  StationRepository (dao, Harsh) ──► the DAOs ──► SQLite
```

The service never writes SQL, and the UI never changes model objects directly. Each layer has one job.

**One action, from click to database:** the user clicks "Set route" →
`MainApp.onSetRoute()` → `service.setRoute("R1")` → `SafetyRules.checkRouteCanBeSet(...)`
(this throws an exception if the route is unsafe) → the service moves and locks the points,
locks the sections and turns the signal green → `saveAndRecord(...)` →
`StationRepository.saveState(...)` saves every row in one transaction, and `logEvent(...)`
adds a row to event_log → back in MainApp, `refresh()` repaints the labels.

---

# Utpal: model, service, exception, util/LogConfig, Main

## model/TrackElement.java

**What it does:** the abstract parent of `TrackSection`, `Point` and `Signal`. It holds what they
all share: an `id` and a `locked` flag. It also declares two abstract methods that every child
must write: `getStatus()` (text for the screen) and `getDisplayColour()` (colour for the screen).

**How it works:**
- There are two constructors (*overloading*). `TrackElement(id)` is used when building a new
  station; `TrackElement(id, locked)` is used by the DAOs to rebuild an element exactly as saved.
  The first one calls the second with `this(id, false)`.
- `Objects.requireNonNull(id, ...)` stops anyone creating an element without an id.
- `lock()` and `release()` set the flag; `isLocked()` reads it.
- It `implements Serializable`, so elements can be written to a snapshot file.

**Lecture topics:** Inheritance, Abstract classes, Constructor overloading, Serialization. (slides __)

**Viva questions**
1. *Why is TrackElement abstract?* There is no such thing as a "plain" track element. It only
   exists to share code; you can only create a section, a point or a signal.
2. *What is `this(id, false)`?* It calls the other constructor of the same class, so the setup
   code is written only once.
3. *Why `protected` constructors?* Only the child classes need to call them (through `super`).
4. *Why no equals()/hashCode()?* Every part of the program uses the same objects from one layout
   map, so comparing "is it the same object?" (the default) is correct.
5. *What does serialVersionUID do?* It is a version number. If the class changes in an
   incompatible way, old snapshot files are rejected instead of being loaded wrongly.

## model/TrackSection.java, model/Point.java, model/Signal.java

**What they do:** the three concrete kinds of element.
- `TrackSection` adds `occupied`. Status: "occupied" / "locked" / "free"; colour: red / yellow / grey.
- `Point` adds `reversed` (false = NORMAL, true = REVERSE). `setReversed(newPosition)` throws
  `PointLockedException` if the point is locked **and** would have to move. Staying in the same
  position is always allowed.
- `Signal` adds `clear` (false = RED, true = GREEN). Colour: green / red.

**How it works:** each class calls `super(id)` or `super(id, locked)` in its constructors and
overrides `getStatus()` and `getDisplayColour()` with simple if/else.
`Point.positionName(boolean)` is a `static` helper that turns true/false into "REVERSE"/"NORMAL".

**Lecture topics:** Inheritance (`extends`, `super`), Method overriding (`@Override`),
Exceptions (`throws`), static methods. (slides __)

**Viva questions**
1. *What is method overriding here?* Each class writes its own `getStatus()`. When the UI calls
   `element.getStatus()`, Java runs the version of the real object (runtime polymorphism).
2. *Why does `setReversed` declare `throws PointLockedException`?* It is a checked exception, so
   the compiler forces callers to deal with the case "the point is locked".
3. *Why is `positionName` static?* It does not use any object's fields; it only converts a boolean to text.
4. *What does `@Override` do?* It asks the compiler to check that we really are overriding a
   parent method. A spelling mistake becomes a compile error.
5. *Why put the colour in the model and not in the UI?* It lets the UI paint any element
   without `instanceof` checks: a simple use of polymorphism. (The trade-off is that the model
   now knows a little about display.)

## model/Route.java

**What it does:** describes one route: id, description, entry signal, the list of sections
(in travel order), and a `HashMap<Point, Boolean>` of required point positions (true = REVERSE).
It also stores run-time state: `active` (is the route set?) and `trainPosition`
(-1 = `NO_TRAIN`, otherwise an index into the section list).

**How the key methods work:**
- `conflictWith(other)`:
  1. For each of my sections, if the other route's section list `contains` it → return
     "both use section Tx".
  2. For each of my points, if the other route also uses it and needs a different position →
     return "they need point Px in different positions".
  3. Otherwise return `null` (no conflict).
- `getPathText()` builds e.g. `"T1 -> T2, P1 NORMAL"` with a for-each loop.
- `getStateText()` returns "not set", "SET, train waiting at SIG_W" or "SET, train in T1".
- `toString()` joins everything into one line; the UI's route list shows it.

**Lecture topics:** Classes and objects, ArrayList, HashMap, `static final` constants, Serializable. (slides __)

**Viva questions**
1. *Why a HashMap for points?* Each point (the key) maps to the one position the route needs (the value).
2. *Why is `NO_TRAIN` `static final`?* It is a constant shared by all routes and can never change.
3. *How does `contains` find a section?* It compares with `equals`. We did not override it, so
   it means "same object", which is right because routes hold the layout's own objects.
4. *Why does `conflictWith` return a String instead of a boolean?* So the error message can say
   *why* the routes conflict.
5. *Which fields are `final`, and why?* The id, description, signal, sections and points never
   change after construction. Only `active` and `trainPosition` change.

## model/EventLogEntry.java

**What it does:** one line of the event log: a `LocalDateTime`, route id, action
(SET, MOVE, CANCEL, POINT, TRACK, REFUSED, SNAPSHOT) and message. All fields are `final`, so an
entry never changes once created.

**Lecture topics:** Classes, `final` fields, LocalDateTime. (slides __)

**Viva questions**
1. *Why are all fields final?* A log entry records what happened; it must never be changed later.
2. *How is the time stored in the database?* As text from `toString()`, e.g. `2026-11-24T16:46:13`,
   and read back with `LocalDateTime.parse`.
3. *Why `withNano(0)` when creating it?* (In the service.) It drops the fractions of a second so the time is easy to read.
4. *Why is it not Serializable?* Entries are saved in the database, never in a snapshot file.
5. *What does toString do here?* It gives the one-line text shown in the event-log list.

## exception/RouteConflictException.java, PointLockedException.java, SectionOccupiedException.java

**What they do:** our three custom **checked** exceptions. Each one extends `Exception` and passes
its message to the parent with `super(message)`.

| Exception | Thrown when |
|-----------|-------------|
| `RouteConflictException` | route already set, conflicts with an active route, a section is locked, cancel/move not allowed, unknown route |
| `SectionOccupiedException` | a section on the route is occupied, or you try to mark a locked section |
| `PointLockedException` | a point is locked, or a train is over it |

**Lecture topics:** Exception handling: custom exceptions, checked vs unchecked, throw/throws. (slides __)

**Viva questions**
1. *Checked vs unchecked?* Checked exceptions extend `Exception`; the compiler forces you to
   catch or declare them. Unchecked ones extend `RuntimeException` and are not checked.
2. *Why checked here?* A refused request is a normal, expected event. The caller (the UI) must handle it.
3. *What does `super(message)` do?* It stores the message in the parent class, so
   `getMessage()` returns it.
4. *Why does an exception have a serialVersionUID?* `Exception` implements `Serializable`, so
   every subclass should have one.
5. *`throw` vs `throws`?* `throw` actually raises an exception object; `throws` in a method
   header warns callers that it might.

## service/StationStore.java

**What it does:** an interface with two methods, `saveState(elements, routes)` and
`logEvent(entry)`. The service only knows this interface; `StationRepository` (Harsh) implements it.

**Viva questions**
1. *Why an interface?* The rules code doesn't depend on SQL. We could swap SQLite for a file
   without changing the service.
2. *What happens when the store is null?* The service runs in memory only (used by tests and when the DB fails).
3. *Why `throws SQLException`?* Saving can fail; the service catches it and logs it.
4. *Who implements it?* `StationRepository` in the dao package.
5. *Which lecture topic?* Interfaces and loose coupling (the DAO pattern idea). (slides __)

## service/StationFactory.java

**What it does:** builds the demo station in memory: 4 sections, 2 points, 2 signals and routes
R1–R4. Same data as schema.sql.

**How it works:** `createDemoLayout()` puts each element in a `HashMap` by id.
`createDemoRoutes(layout)` calls `makeRoute(...)`, which takes the objects **from the layout**
(with casts like `(TrackSection) layout.get("T1")`), so routes and layout share the same objects.
`createDemoService()` combines both, with no database.

**Viva questions**
1. *Why take objects from the layout instead of creating new ones?* If a route had its own copy
   of T1, locking it would not lock the T1 that the screen shows.
2. *Why the cast?* The map stores `TrackElement`; we know "T1" is a `TrackSection`, so we cast to use its methods.
3. *Why static methods?* The factory has no state; it just builds objects.
4. *Where is it used?* In every test in Main, and in MainApp if the database cannot be opened.
5. *Topic?* static methods, HashMap/ArrayList, casting. (slides __)

## service/SafetyRules.java

**What it does:** all the safety checks in one place. These methods **only check**; they never
change anything. If a rule is broken they throw an exception; if they return normally, the action is safe.

**How the key methods work:**
- `checkRouteCanBeSet(route, allRoutes)`, in this order:
  1. Already active? → `RouteConflictException`.
  2. Any section occupied? → `SectionOccupiedException`.
  3. For every *active* route, `route.conflictWith(other)` not null? → `RouteConflictException`.
  4. Any section locked? → `RouteConflictException`.
  5. For every point that must **move**, call `checkPointCanMove`.
- `checkPointCanMove(point, allRoutes)`: locked → `PointLockedException`; any section at the
  point occupied → `PointLockedException`.
- `sectionsAtPoint(point, allRoutes)` finds the section the point lies in: it starts with
  the sections of the first route through the point, then uses `retainAll` with every other
  such route, keeping only the common ones. P1: {T1,T2} ∩ {T1,T3} = {T1}.

**Viva questions**
1. *Why separate checks from actions?* So we check everything *before* changing anything. A
   refused route leaves the station untouched.
2. *What does `retainAll` do?* It removes every item not also in the other collection
   (set intersection).
3. *Why use a HashSet there?* The result is a set of distinct sections; order doesn't matter.
4. *Why does the order of checks matter?* It decides which message you see. For example, an
   occupied section is reported before a conflict.
5. *Why is a point only checked if it must move?* A locked point already in the right position
   is no danger. Nothing moves.

## service/InterlockingService.java

**What it does:** the "brain". It carries out every action, writes it to the event log and saves
the state. The UI and the tests only call this class.

**How the key methods work:**
- `setRoute(id)`: find the route → `SafetyRules.checkRouteCanBeSet` (on an exception: log
  "REFUSED" and rethrow it) → move and lock each point → lock each section → `active = true`
  → signal GREEN → `saveAndRecord`.
- `cancelRoute(id)`: refuse if not set or a train is on it → signal RED → release sections
  and points → not active → save.
- `moveTrain(id)`, one call = one section:
  - no train yet: signal RED, first section occupied, position 0;
  - otherwise: the section being left becomes clear and is released; if there is a next
    section it becomes occupied; if not, the route finishes and its points are released.
- `throwPoint(id)`: `checkPointCanMove`, then flip `reversed`.
- `setSectionOccupied(id, value)`: refused if the section is locked (only the train may change it).
- `saveSnapshot` / `loadSnapshot`: delegate to `SnapshotFile`. The state is only replaced after the whole file was read.
- `saveAndRecord`: calls `store.saveState(...)` (if there is a store), then `addToLog`.
- `addToLog`: creates an `EventLogEntry`, adds it to the list, writes it to the Logger, and
  sends it to the store.
- Getters such as `getRoutes()` return **copies** of the lists, so callers cannot add or remove items.

**Viva questions**
1. *Why is the exception caught and then thrown again in setRoute?* To write "REFUSED" in the
   log first, then let the UI show the error.
2. *What if the database fails while saving?* The `SQLException` is caught and logged; the
   interlocking keeps working in memory.
3. *Why does the signal turn red when the train enters?* So no second train can follow onto a route that is already in use.
4. *Why release sections one by one?* Other routes can use the track behind the train
   sooner (test 7: R3 can be set once T1 is released).
5. *Why can't a route with a train be cancelled?* The driver has passed a green signal and
   relies on the points staying locked.

## service/SnapshotFile.java

**What it does:** writes the whole station to a file and reads it back.

**How it works:** `save` opens an `ObjectOutputStream` in try-with-resources, writes the number of
elements, then each element, then the number of routes, then each route. `load` reads in the
same order: `readInt()` tells it how many `readObject()` calls to make, and each object is cast
to `TrackElement` or `Route`.

**Viva questions**
1. *What is serialization?* Turning an object into bytes so it can be saved or sent, and rebuilt later.
2. *Why write a count first?* So the reader knows how many objects to read.
3. *Why write everything into one stream?* Java remembers objects already written in the
   same stream. When a route refers to T1 again, the same T1 object is restored, not a copy
   (test 14 checks this).
4. *What is try-with-resources?* `try (stream ...) { }` closes the stream automatically, even on error.
5. *Which exceptions can load throw?* `IOException` (file problems) and `ClassNotFoundException`
   (the class of a saved object cannot be found).

## util/LogConfig.java

**What it does:** sets up java.util.logging so everything our classes log goes into `interlocking.log`.

**How it works:** it gets the logger named `com.trains.interlocking`, sets its level to INFO,
turns off the console with `setUseParentHandlers(false)`, and adds a `FileHandler` (append mode)
with a `SimpleFormatter`. The `configured` flag makes sure this happens only once. The logger of
`InterlockingService` is a *child* of this logger (by name), so its messages reach the file.

**Viva questions**
1. *What is a Handler?* It decides where log messages go (file, console...).
2. *Why SimpleFormatter?* FileHandler writes XML by default; SimpleFormatter gives readable text.
3. *What does Level.INFO mean?* INFO, WARNING and SEVERE messages are kept; finer ones are dropped.
4. *Why keep the logger in a static field?* Loggers can be garbage collected if nothing holds them; the handler would be lost.
5. *Why the `configured` flag?* Calling configure twice would add two handlers and write every line twice.

## Main.java (test harness)

**What it does:** runs 15 scenarios, each in its own method, and prints `PASS`/`FAIL` and then e.g.
`15/15 passed`.

**How it works:** each test makes a fresh station with `StationFactory.createDemoService()`,
does something, and calls `report(name, ok)`. Tests that expect a refusal put the call in
`try`, call `report(name, false)` straight after it (that line is reached only if nothing was
thrown), and call `report(name, true)` in the matching `catch`. Tests 2–5 share the helper
`expectConflict`. Test 15 uses a temporary SQLite file, opens it twice and checks the state
survived, including a direct `TrackSectionDao.findById("T4")`.

**Viva questions**
1. *Why not JUnit?* It is not in our syllabus; a main method with if/else checks does the same job here.
2. *Why a fresh station in each test?* So one test cannot affect another.
3. *How does a "refused" test pass?* If the right exception is thrown, the catch block reports PASS.
4. *Why `catch (Exception e)` at the end?* To report a wrong or unexpected exception as FAIL instead of crashing.
5. *How does test 15 prove persistence?* It closes the first connection, opens a new one, and
   loads everything again from the file.

---

# Harsh: dao, util/DatabaseConnection, util/AppConfig, schema.sql

## resources/schema.sql

**What it does:** creates 7 tables and inserts the demo station.

| Table | Holds |
|-------|-------|
| `track_section` | id, occupied, locked |
| `track_point` | id, reversed, locked |
| `track_signal` | id, clear |
| `route` | id, description, entry_signal_id, active, train_position |
| `route_section` | which sections a route uses, and in which **position** (order) |
| `route_point` | which points a route needs, and whether reversed |
| `event_log` | id (auto number), timestamp, route_id, action, message |

`CREATE TABLE IF NOT EXISTS` and `INSERT OR IGNORE` make the script safe to run on **every**
start-up: existing tables and saved state are kept. Booleans are stored as 0/1 because SQLite
has no boolean type.

**Viva questions**
1. *Why IF NOT EXISTS / OR IGNORE?* So restarting the app does not wipe the saved state.
2. *Why separate route_section and route_point tables?* A route has many sections and many
   points (one-to-many), so they need their own rows.
3. *Why a `position` column?* The train must run over the sections in the right order; we use `ORDER BY position`.
4. *What is a composite primary key?* `PRIMARY KEY (route_id, position)`: the pair must be unique.
5. *What does AUTOINCREMENT do?* SQLite gives each log row the next number automatically.

## util/AppConfig.java

**What it does:** reads `app.properties` once (in a `static` block) into a `Properties` object,
and returns `db.url`, `station.name` and `log.file`, with a default value if a key is missing.

**Viva questions**
1. *What is a static block?* Code that runs once, when the class is first used.
2. *Why `getResourceAsStream` and not a file path?* The file is on the classpath (`src/main/resources`), wherever the program is run from.
3. *What does `getProperty(key, default)` do?* It returns the value, or the default if the key is missing.
4. *Why keep settings in a file?* You can change the database name without recompiling.
5. *What if the file is missing?* `in` is null, nothing is loaded, and the defaults are used.

## util/DatabaseConnection.java

**What it does:** opens connections and creates the schema.

**How it works:**
- `getConnection()` uses the URL from AppConfig; `getConnection(url)` is an overload the tests
  use for a temporary file. Both call `DriverManager.getConnection(url)`.
- `initialiseSchema(conn)`: reads schema.sql with a `BufferedReader` (skipping `--` comment
  lines), splits it on `;`, then **in one transaction** runs each statement with a
  `PreparedStatement`. On any error it calls `rollback()`; `finally` turns auto-commit back on.

**Viva questions**
1. *What does DriverManager do?* It finds the JDBC driver for the URL (here SQLite) and opens a `Connection`.
2. *Why one transaction for the schema?* All or nothing: we never end up with half the tables.
3. *Why `finally` for setAutoCommit(true)?* It must run whether the commit worked or failed.
4. *Why skip comment lines?* A comment could contain a `;`, which would break the split.
5. *What is the JDBC URL?* `jdbc:sqlite:interlocking.db`: the protocol, the driver, and the file.

## dao/Dao.java

**What it does:** the generic DAO interface: `findAll()`, `findById(id)` returning `Optional<T>`,
and `update(item)`. `T` is replaced by a real class, e.g. `Dao<Point>`.

**Viva questions**
1. *What is the DAO pattern?* One class per table that hides all the SQL, so the rest of the program only works with objects.
2. *What is `<T>`?* A type parameter: one interface works for any class, and the compiler checks the types.
3. *Why Optional?* `findById` may find nothing; Optional makes the caller think about the
   "empty" case instead of getting `null`.
4. *Why only three methods?* Those are all the app needs. Rows are created by schema.sql, never deleted.
5. *Name the implementations.* TrackSectionDao, PointDao, SignalDao, RouteDao. (EventLogDao is separate.)

## dao/TrackSectionDao.java, PointDao.java, SignalDao.java

**What they do:** read and update one table each. They have the same shape:
- constructor stores the `Connection`;
- `findAll()`: `SELECT * ... ORDER BY id`, loop over the `ResultSet`, turn each row into an object with `fromRow`;
- `findById(id)`: `SELECT ... WHERE id = ?`, `setString(1, id)`, return `Optional.of(...)` or `Optional.empty()`;
- `update(item)`: `UPDATE ... SET ... WHERE id = ?`, with `setInt(…, flag ? 1 : 0)`;
- `fromRow(rs)` calls the full constructor, e.g. `new Point(id, reversed, locked)`.

**Viva questions**
1. *Why PreparedStatement and not string concatenation?* The `?` placeholders keep values
   separate from the SQL. This stops SQL injection and handles quotes safely.
2. *What is a ResultSet?* A cursor over the query's rows; `next()` moves to the next row and returns false at the end.
3. *Why `flag ? 1 : 0`?* SQLite has no boolean type, so we store 1 or 0.
4. *Why try-with-resources?* Statements and result sets are closed automatically.
5. *Why does PointDao use `new Point(id, reversed, locked)`?* `setReversed` can throw a checked
   exception; the constructor sets the value directly.

## dao/RouteDao.java

**What it does:** builds `Route` objects from three tables and saves a route's state.

**How it works:**
- The constructor gets the `layout` map so ids can be turned into the **same** objects already loaded.
- `findAll()`: for each row of `route` → look up the signal, `loadSections(id)` (ordered by
  position), `loadPoints(id)` → construct the Route → set `active` and `trainPosition`.
- `findById(id)`: loops over `findAll()` (only four routes, so this is simplest).
- `update(route)`: saves only `active` and `train_position`; the path never changes.

**Viva questions**
1. *Why does RouteDao need the layout?* A route must hold the same section objects as the layout, or locking would not show on screen.
2. *Why ORDER BY position?* The train must visit the sections in the right order.
3. *Why does update not save sections?* They never change; only the state does.
4. *Why a cast like `(TrackSection) layout.get(...)`?* The map holds `TrackElement`; we know which kind it is.
5. *Is `new RouteDao(conn, null)` in StationRepository safe?* Yes. `update()` never uses the layout.

## dao/EventLogDao.java

**What it does:** `insert(entry)` adds a row to `event_log`; `findRecent(limit)` returns the newest
entries, oldest first.

**How it works:** the SQL asks for `ORDER BY id DESC LIMIT ?` (newest first). Each row is added with
`LinkedList.addFirst`, which reverses the order, so the list ends oldest → newest.

**Viva questions**
1. *Why isn't it a Dao<T>?* Log rows are only ever added, never updated, so the interface does not fit.
2. *Why LinkedList?* `addFirst` is quick and makes the reversing easy.
3. *What does LIMIT do?* It returns at most that many rows.
4. *How is the time stored?* As text; it is read back with `LocalDateTime.parse`.
5. *Where is it used?* `StationRepository.logEvent` and `recentEvents` (MainApp loads 100 at start-up).

## dao/StationRepository.java

**What it does:** implements `StationStore`. It loads the whole station and saves it in **one transaction**.

**How it works:**
- `loadLayout()`: all sections, points and signals into one `HashMap` by id.
- `loadRoutes(layout)`: `new RouteDao(conn, layout).findAll()`.
- `saveState(elements, routes)`:
  1. `conn.setAutoCommit(false)`: start the transaction;
  2. for each element, `instanceof` picks the right DAO and calls `update`;
  3. update every route;
  4. `conn.commit()`; on `SQLException` → `conn.rollback()` and rethrow;
  5. `finally` → `setAutoCommit(true)`.

**Viva questions**
1. *What is a transaction?* A group of changes that are saved all together or not at all.
2. *Why one transaction here?* If the app crashed halfway, the DB could show a route set but
   its points not locked. That is unsafe, so it must be all or nothing.
3. *What does rollback do?* It undoes every change since `setAutoCommit(false)`.
4. *Why is `instanceof` needed here?* Each element type lives in a different table, so we must know the type to pick the DAO.
5. *How does the service use this class without knowing SQL?* Through the `StationStore` interface.

---

# Aryan: ui

## ui/Launcher.java

**What it does:** a normal class with `main()` that calls `Application.launch(MainApp.class, args)`.

**Viva questions**
1. *Why not run MainApp directly?* When a class that extends Application is started from the
   classpath, Java may say "JavaFX runtime components are missing". A plain launcher class avoids this.
2. *What does `Application.launch` do?* It starts JavaFX, then calls `init()`, `start()` and, on exit, `stop()`.
3. *What is `MainApp.class`?* An object that describes the MainApp class, so JavaFX knows which app to create.
4. *Which command runs the app?* `mvn javafx:run`, or run Launcher in the IDE.
5. *Topic?* JavaFX application lifecycle. (slides __)

## ui/MainApp.java

**What it does:** the window. The top shows the title and where data is saved. The middle shows
a **row of coloured labels** for the track (west to east: SIG_W, T1, P1, T2, T3, P2, T4, SIG_E),
a legend, the route list and the event log. The right side has all the controls.

**How it works:**
- **Lifecycle:**
  - `init()` runs before the window exists. It sets up logging, opens the database, runs the
    schema, loads the layout and routes, creates the service and loads the last 100 events.
    If the database fails, it falls back to the in-memory demo station.
  - `start(stage)` builds the layout: a `BorderPane` with top = `VBox`, centre = `VBox`,
    right = the controls `VBox`. It calls `refresh()` and shows the stage.
  - `stop()` closes the database connection.
- **`buildTrackRow()`** creates one `Label` per id in `TRACK_ORDER`, puts it in an `HBox`, and
  remembers it in a `HashMap<String, Label>`.
- **`buildControls()`** creates the ComboBoxes (routes, points, sections), buttons, and the snapshot
  `TextField`. Each button gets a simple lambda: `setButton.setOnAction(e -> onSetRoute());`
- **Handlers** (`onSetRoute`, `onMoveTrain`, ...) all have the same shape:
  `try { service.something(...); refresh(); } catch (Exception e) { showError(title, e.getMessage()); }`
- **`refresh()`** reads the service and repaints. For each label it sets the text to id + status
  and the colour with `setStyle("-fx-background-color: " + element.getDisplayColour() + ...)`.
  It refills both ListViews, scrolls the log to the newest line, and puts the last message in "Last result".
- **`showError`** refreshes (so the REFUSED line appears in the log) and shows an `Alert` with the exception message.

**Lecture topics:** JavaFX: Application lifecycle, Stage/Scene, BorderPane/VBox/HBox, Label,
Button, ComboBox, ListView, TextField, Alert, event handling with lambdas, setStyle. (slides __)

**Viva questions**
1. *What is the difference between init() and start()?* `init()` runs first and must not create
   windows, so we load data there. `start()` gets the Stage and builds the UI.
2. *What is `e -> onSetRoute()`?* A lambda: a short way to write the event handler that runs when the button is clicked.
3. *How does the UI know an element's colour without instanceof?* Each element class overrides
   `getDisplayColour()`; the label calls it (polymorphism).
4. *Why catch `Exception` in the handlers?* `setRoute` can throw three different checked
   exceptions; they are all shown the same way, so one catch is simpler.
5. *What does `refresh()` do, and when is it called?* It copies the current state of the
   service onto the screen. It is called after every successful action and after every error.
