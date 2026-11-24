-- Railway Interlocking — Two-Platform Demo Station
--
--               T3 (loop platform)
--           /----------------------\
--  T1 ---- P1 ------ T2 (main) ----- P2 ---- T4
--  [SIG_W]                                [SIG_E]
--
-- Track sections: T1, T2, T3, T4
-- Points:         P1, P2
-- Signals:        SIG_W, SIG_E
-- Routes:
--   R1  West -> Main   T1, T2   P1 normal
--   R2  West -> Loop   T1, T3   P1 reverse
--   R3  East -> Main   T4, T2   P2 normal
--   R4  East -> Loop   T4, T3   P2 reverse
--
-- Booleans are stored as INTEGER 0/1. Note: comment lines must not contain semicolons
-- inside statements, the loader splits the script on semicolons.

CREATE TABLE IF NOT EXISTS track_section (
    id          TEXT PRIMARY KEY,
    description TEXT NOT NULL DEFAULT '',
    occupied    INTEGER NOT NULL DEFAULT 0,
    locked      INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS track_point (
    id       TEXT PRIMARY KEY,
    reversed INTEGER NOT NULL DEFAULT 0,
    locked   INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS track_signal (
    id     TEXT PRIMARY KEY,
    clear  INTEGER NOT NULL DEFAULT 0,
    locked INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS route (
    id              TEXT PRIMARY KEY,
    description     TEXT NOT NULL DEFAULT '',
    entry_signal_id TEXT NOT NULL REFERENCES track_signal(id),
    active          INTEGER NOT NULL DEFAULT 0,
    train_position  INTEGER NOT NULL DEFAULT -1
);

CREATE TABLE IF NOT EXISTS route_section (
    route_id   TEXT NOT NULL REFERENCES route(id) ON DELETE CASCADE,
    seq        INTEGER NOT NULL,
    section_id TEXT NOT NULL REFERENCES track_section(id),
    PRIMARY KEY (route_id, seq)
);

CREATE TABLE IF NOT EXISTS route_point (
    route_id TEXT NOT NULL REFERENCES route(id) ON DELETE CASCADE,
    point_id TEXT NOT NULL REFERENCES track_point(id),
    reversed INTEGER NOT NULL,
    PRIMARY KEY (route_id, point_id)
);

CREATE TABLE IF NOT EXISTS event_log (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp TEXT NOT NULL,
    route_id  TEXT NOT NULL DEFAULT '',
    action    TEXT NOT NULL,
    message   TEXT NOT NULL
);

-- Seed data: the demo station layout
INSERT OR IGNORE INTO track_section (id, description) VALUES ('T1', 'West approach');
INSERT OR IGNORE INTO track_section (id, description) VALUES ('T2', 'Main platform');
INSERT OR IGNORE INTO track_section (id, description) VALUES ('T3', 'Loop platform');
INSERT OR IGNORE INTO track_section (id, description) VALUES ('T4', 'East approach');

INSERT OR IGNORE INTO track_point (id) VALUES ('P1');
INSERT OR IGNORE INTO track_point (id) VALUES ('P2');

INSERT OR IGNORE INTO track_signal (id) VALUES ('SIG_W');
INSERT OR IGNORE INTO track_signal (id) VALUES ('SIG_E');

INSERT OR IGNORE INTO route (id, description, entry_signal_id) VALUES ('R1', 'West -> Main', 'SIG_W');
INSERT OR IGNORE INTO route (id, description, entry_signal_id) VALUES ('R2', 'West -> Loop', 'SIG_W');
INSERT OR IGNORE INTO route (id, description, entry_signal_id) VALUES ('R3', 'East -> Main', 'SIG_E');
INSERT OR IGNORE INTO route (id, description, entry_signal_id) VALUES ('R4', 'East -> Loop', 'SIG_E');

INSERT OR IGNORE INTO route_section (route_id, seq, section_id) VALUES ('R1', 0, 'T1');
INSERT OR IGNORE INTO route_section (route_id, seq, section_id) VALUES ('R1', 1, 'T2');
INSERT OR IGNORE INTO route_section (route_id, seq, section_id) VALUES ('R2', 0, 'T1');
INSERT OR IGNORE INTO route_section (route_id, seq, section_id) VALUES ('R2', 1, 'T3');
INSERT OR IGNORE INTO route_section (route_id, seq, section_id) VALUES ('R3', 0, 'T4');
INSERT OR IGNORE INTO route_section (route_id, seq, section_id) VALUES ('R3', 1, 'T2');
INSERT OR IGNORE INTO route_section (route_id, seq, section_id) VALUES ('R4', 0, 'T4');
INSERT OR IGNORE INTO route_section (route_id, seq, section_id) VALUES ('R4', 1, 'T3');

INSERT OR IGNORE INTO route_point (route_id, point_id, reversed) VALUES ('R1', 'P1', 0);
INSERT OR IGNORE INTO route_point (route_id, point_id, reversed) VALUES ('R2', 'P1', 1);
INSERT OR IGNORE INTO route_point (route_id, point_id, reversed) VALUES ('R3', 'P2', 0);
INSERT OR IGNORE INTO route_point (route_id, point_id, reversed) VALUES ('R4', 'P2', 1);
