-- schema.sql : tables and starting data for the Two-Platform Demo Station
-- Owner: Harsh.  Lecture deck: JDBC / SQL basics.
--
--               T3 (loop platform)
--           /----------------------\
--  T1 ---- P1 ------ T2 (main) ----- P2 ---- T4
--  [SIG_W]                                [SIG_E]
--
-- true/false values are stored as INTEGER 1/0.
-- "INSERT OR IGNORE" adds a row only if it is not there yet, so running this
-- file again on every start-up never wipes the saved state.
-- The Java loader splits this file on semicolons, so comments must not contain one.

CREATE TABLE IF NOT EXISTS track_section (
    id       TEXT PRIMARY KEY,
    occupied INTEGER NOT NULL DEFAULT 0,
    locked   INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS track_point (
    id       TEXT PRIMARY KEY,
    reversed INTEGER NOT NULL DEFAULT 0,
    locked   INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS track_signal (
    id    TEXT PRIMARY KEY,
    clear INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS route (
    id              TEXT PRIMARY KEY,
    description     TEXT NOT NULL,
    entry_signal_id TEXT NOT NULL,
    active          INTEGER NOT NULL DEFAULT 0,
    train_position  INTEGER NOT NULL DEFAULT -1
);

-- which sections a route uses, in the order the train runs over them
CREATE TABLE IF NOT EXISTS route_section (
    route_id   TEXT NOT NULL,
    position   INTEGER NOT NULL,
    section_id TEXT NOT NULL,
    PRIMARY KEY (route_id, position)
);

-- which points a route needs, and in which position (1 = reverse)
CREATE TABLE IF NOT EXISTS route_point (
    route_id TEXT NOT NULL,
    point_id TEXT NOT NULL,
    reversed INTEGER NOT NULL,
    PRIMARY KEY (route_id, point_id)
);

CREATE TABLE IF NOT EXISTS event_log (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp TEXT NOT NULL,
    route_id  TEXT NOT NULL,
    action    TEXT NOT NULL,
    message   TEXT NOT NULL
);

INSERT OR IGNORE INTO track_section (id) VALUES ('T1');
INSERT OR IGNORE INTO track_section (id) VALUES ('T2');
INSERT OR IGNORE INTO track_section (id) VALUES ('T3');
INSERT OR IGNORE INTO track_section (id) VALUES ('T4');

INSERT OR IGNORE INTO track_point (id) VALUES ('P1');
INSERT OR IGNORE INTO track_point (id) VALUES ('P2');

INSERT OR IGNORE INTO track_signal (id) VALUES ('SIG_W');
INSERT OR IGNORE INTO track_signal (id) VALUES ('SIG_E');

INSERT OR IGNORE INTO route (id, description, entry_signal_id) VALUES ('R1', 'West -> Main', 'SIG_W');
INSERT OR IGNORE INTO route (id, description, entry_signal_id) VALUES ('R2', 'West -> Loop', 'SIG_W');
INSERT OR IGNORE INTO route (id, description, entry_signal_id) VALUES ('R3', 'East -> Main', 'SIG_E');
INSERT OR IGNORE INTO route (id, description, entry_signal_id) VALUES ('R4', 'East -> Loop', 'SIG_E');

INSERT OR IGNORE INTO route_section (route_id, position, section_id) VALUES ('R1', 0, 'T1');
INSERT OR IGNORE INTO route_section (route_id, position, section_id) VALUES ('R1', 1, 'T2');
INSERT OR IGNORE INTO route_section (route_id, position, section_id) VALUES ('R2', 0, 'T1');
INSERT OR IGNORE INTO route_section (route_id, position, section_id) VALUES ('R2', 1, 'T3');
INSERT OR IGNORE INTO route_section (route_id, position, section_id) VALUES ('R3', 0, 'T4');
INSERT OR IGNORE INTO route_section (route_id, position, section_id) VALUES ('R3', 1, 'T2');
INSERT OR IGNORE INTO route_section (route_id, position, section_id) VALUES ('R4', 0, 'T4');
INSERT OR IGNORE INTO route_section (route_id, position, section_id) VALUES ('R4', 1, 'T3');

INSERT OR IGNORE INTO route_point (route_id, point_id, reversed) VALUES ('R1', 'P1', 0);
INSERT OR IGNORE INTO route_point (route_id, point_id, reversed) VALUES ('R2', 'P1', 1);
INSERT OR IGNORE INTO route_point (route_id, point_id, reversed) VALUES ('R3', 'P2', 0);
INSERT OR IGNORE INTO route_point (route_id, point_id, reversed) VALUES ('R4', 'P2', 1);
