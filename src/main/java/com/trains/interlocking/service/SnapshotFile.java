/*
 * SnapshotFile.java
 * Saves the whole station (every element and every route) to a file with
 * ObjectOutputStream, and reads it back with ObjectInputStream.
 * Lecture decks: Java I/O Streams, Serialization (Serializable, serialVersionUID).
 * Owner: Utpal
 */
package com.trains.interlocking.service;

import com.trains.interlocking.model.Route;
import com.trains.interlocking.model.TrackElement;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class SnapshotFile {

    public static void save(String fileName, HashMap<String, TrackElement> layout,
                            ArrayList<Route> routes) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName))) {
            // Everything goes into ONE stream, so when it is read back a route and
            // the layout still share the same section objects.
            out.writeInt(layout.size());
            for (TrackElement element : layout.values()) {
                out.writeObject(element);
            }
            out.writeInt(routes.size());
            for (Route route : routes) {
                out.writeObject(route);
            }
        }
    }

    /** Reads a snapshot into the two EMPTY collections passed in. */
    public static void load(String fileName, HashMap<String, TrackElement> layout,
                            ArrayList<Route> routes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName))) {
            // We wrote a count before each group, so we know how many objects to read.
            int elementCount = in.readInt();
            for (int i = 0; i < elementCount; i++) {
                TrackElement element = (TrackElement) in.readObject();
                layout.put(element.getId(), element);
            }
            int routeCount = in.readInt();
            for (int i = 0; i < routeCount; i++) {
                routes.add((Route) in.readObject());
            }
        }
    }
}
