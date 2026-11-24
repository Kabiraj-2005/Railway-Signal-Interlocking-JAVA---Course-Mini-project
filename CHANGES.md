# Changes: simplification to course level

This file lists every change made when the project was simplified so that each line can be
explained from the UCS553 lecture decks. Use it to update the project report.

The version before simplification is saved in the commit **"Backup before simplification"**.

## Line counts

| File | Status | Old lines | New lines |
|------|--------|----------:|----------:|
| `Main.java` | rewritten | 305 | 265 |
| `model/TrackElement.java` | rewritten | 67 | 56 |
| `model/TrackSection.java` | rewritten | 43 | 54 |
| `model/Point.java` | rewritten | 53 | 61 |
| `model/Signal.java` | rewritten | 35 | 48 |
| `model/Route.java` | rewritten | 181 | 136 |
| `model/EventLogEntry.java` | rewritten | 51 | 47 |
| `exception/RouteConflictException.java` | kept (header comment added) | 13 | 19 |
| `exception/PointLockedException.java` | kept (header comment added) | 13 | 19 |
| `exception/SectionOccupiedException.java` | kept (header comment added) | 13 | 19 |
| `service/InterlockingService.java` | rewritten | 439 | 251 |
| `service/SafetyRules.java` | **new** (checks moved out of the service) | 0 | 92 |
| `service/SnapshotFile.java` | **new** (serialization moved out of the service) | 0 | 55 |
| `service/StationFactory.java` | rewritten | 72 | 69 |
| `service/StationStore.java` | rewritten | 21 | 23 |
| `dao/Dao.java` | rewritten | 23 | 25 |
| `dao/TrackSectionDao.java` | rewritten | 88 | 67 |
| `dao/PointDao.java` | rewritten | 92 | 67 |
| `dao/SignalDao.java` | rewritten | 86 | 66 |
| `dao/RouteDao.java` | rewritten | 223 | 106 |
| `dao/EventLogDao.java` | rewritten | 64 | 55 |
| `dao/StationRepository.java` | rewritten | 93 | 94 |
| `util/AppConfig.java` | rewritten | 47 | 42 |
| `util/DatabaseConnection.java` | rewritten | 81 | 68 |
| `util/LogConfig.java` | rewritten | 43 | 39 |
| `ui/MainApp.java` | rewritten | 410 | 279 |
| `ui/Launcher.java` | kept (header comment added) | 13 | 18 |
| `ui/TrackDiagram.java` | **deleted** | 174 | 0 |
| `resources/schema.sql` | rewritten | 98 | 91 |
| `resources/app.properties` | unchanged | 3 | 3 |
| **All Java files** | | **2,743** | **2,140** |

Of the 2,140 Java lines, **1,384 are actual code**; the rest are the required header comments,
"why" comments, imports and blank lines. The old version had 1,909 lines of code.

## Changes in behaviour

1. **Cancel and move refusals now throw checked exceptions.** `cancelRoute` and `moveTrain`
   used to throw `IllegalStateException`. They now throw `RouteConflictException`
   (for "route not set" or "train on route").
2. **Marking a locked section** now throws `SectionOccupiedException` (it was `IllegalStateException`).
3. **Unknown route id** throws `RouteConflictException` ("There is no route called X") instead of
   `IllegalArgumentException`.
4. **Point release:** when a route ends, its points are simply released. The old "is another
   active route still using this point?" check was removed. It could never be true in this
   station, because the two routes through a point share its section, so they can never both be set.
5. **The track picture** is now a row of coloured labels instead of a drawn diagram, and
   you can no longer click the picture. Use the "Throw point" and "Mark occupied / Mark clear"
   buttons instead.
6. **Routes and the event log** are shown in `ListView`s (one line per item) instead of tables.
   The element status table was removed; each element's status is shown on its coloured label.
7. **The occupancy CheckBox** was replaced by two buttons, "Mark occupied" and "Mark clear".
8. **Log file format** is now the default `SimpleFormatter` format (two lines per message), and
   log messages go only to the file, not the console.
9. **The `-Ddb.url=...` command-line override was removed.** Settings come only from
   `app.properties`. (The tests still use a temporary database, through
   `DatabaseConnection.getConnection(url)`.)
10. **Event log times** are shown as `2026-11-24T16:46:13` (`LocalDateTime.toString()`).

## Changes in structure

### model
- Removed the `equals()`/`hashCode()` overrides in `TrackElement` and `Route`. Every part of the
  program uses the same objects from one layout map, so the default "same object" comparison
  is correct. A comment in `TrackElement` explains this.
- Removed `getType()` and `TrackSection`'s description field.
- Added the abstract method `getDisplayColour()`. Each element picks its own colour, so the UI
  needs no `instanceof`.
- Added constructor overloads such as `Point(id, reversed, locked)` so the DAOs can rebuild
  objects without calling `setReversed()`, which throws a checked exception.
- `Route` now stores `ArrayList`/`HashMap` directly and returns them without copying. A route
  never changes after it is built.
- `EventLogEntry` is no longer `Serializable`, because it is never written to a snapshot.

### service
- The safety checks moved into the new class **`SafetyRules`**, which only checks and never
  changes anything. `InterlockingService` only carries out actions.
- Serialization moved into the new class **`SnapshotFile`**. It writes a count followed by each
  object, so reading back needs only simple casts and no generic casts.
- Removed `LinkedHashMap`, `Collections.unmodifiableMap/List`, `Comparator` and the method
  reference `Route::getId`. Getters return copies (`new ArrayList<>(...)`), with a comment saying why.
- Removed the multi-catch `catch (A | B | C e)`; there are now three plain catch blocks.
- The private helper `record(...)` was renamed `addToLog(...)`, so it is not confused with Java "records".
- `saveSnapshot`/`loadSnapshot` take a file name (`String`) instead of a `File`.

### dao
- `Dao<T>` now has three methods: `findAll`, `findById` (returns `Optional`) and `update`.
  The unused `save` and `delete` were removed.
- `RouteDao`: removed the generic method `<E extends TrackElement> E lookup(...)`; it now uses a
  plain cast. Removed the "transaction ownership" logic (`ownTx`, `commitIfOwn`, `rollbackIfOwn`).
  Transactions now happen only in `StationRepository.saveState` and
  `DatabaseConnection.initialiseSchema`.
- `RouteDao.update` saves only the route's state (active flag and train position).
- `EventLogDao.findRecent` uses `LinkedList.addFirst` to put the newest rows last.
- Only `PreparedStatement` is used; `Statement` was removed everywhere.

### util
- `AppConfig`: removed the `System.getProperty` override.
- `LogConfig`: removed the `System.setProperty` format string and the console-handler loop.
  `setUseParentHandlers(false)` keeps messages out of the console.
- `DatabaseConnection`: removed the `PRAGMA foreign_keys` statement and runs each schema
  statement with a `PreparedStatement`.

### schema.sql
- Removed the `REFERENCES`/`ON DELETE CASCADE` foreign keys, which were not enforced anyway.
- Removed `track_section.description` and `track_signal.locked`.
- Renamed `route_section.seq` to `position`.
- **Delete any old `interlocking.db` before running**, because the old tables have different columns.

### ui
- Deleted `TrackDiagram.java` (it drew the track with `javafx.scene.shape`).
- `MainApp`: removed `TableView`, `SimpleStringProperty`, cell value factories, `CheckBox`,
  `ChangeListener`, the `UiAction` functional interface and the `Font`/`Color`/`Rectangle`
  classes. Colours are set with `setStyle`. Every button has its own short handler method with
  a plain try/catch.

### Main.java (tests)
- 15 scenarios, each in its own clearly named method. Prints `PASS`/`FAIL` and then `15/15 passed`.
- Removed the `Action` functional interface.
