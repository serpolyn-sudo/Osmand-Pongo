package com.osmand.gpsradar;

import java.util.HashMap;
import java.util.Map;

/** Settings are keyed by OsmAnd profile; values must be persisted by the host app. */
public final class RadarProfileSettings {
    public enum Mode { STOCK, EXTENDED }
    public enum Database { CAR, TRUCK }
    private final Map<String, Profile> profiles = new HashMap<>();

    public static Database databaseFor(String profileKey) {
        return "truck".equalsIgnoreCase(profileKey) ? Database.TRUCK : Database.CAR;
    }
    public Profile forProfile(String key) {
        String id = key == null ? "default" : key;
        return profiles.computeIfAbsent(id, k -> new Profile(databaseFor(k)));
    }
    public static final class Profile {
        public final Database database;
        public Mode mode = Mode.STOCK;
        public boolean showAllMarkers = true;
        public boolean enabled = true;
        public int speedToleranceKmh = 0;
        public int[] distancesMeters = {700, 300, 150};
        public final boolean[] typeEnabled = new boolean[64];
        public Profile(Database database) {
            this.database = database;
            java.util.Arrays.fill(typeEnabled, true);
        }
        public void changeDistances(int[] values) {
            if (values == null || values.length == 0 || values.length > 8)
                throw new IllegalArgumentException("Invalid distances");
            for (int value: values) if (value < 10 || value > 3000)
                throw new IllegalArgumentException("Invalid distance in meters");
            distancesMeters = values.clone();
        }
    }
}
