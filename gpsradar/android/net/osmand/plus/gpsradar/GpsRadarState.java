package net.osmand.plus.gpsradar;

/**
 * Thread-safe UI bridge. Snapshot is updated only from verified camera data;
 * average=-1 means unknown and must never be presented as 0 km/h.
 */
public final class GpsRadarState {
    public static final class Snapshot {
        public final GpsRadarPoint point;
        public final int distanceMeters, speedLimitKmh, averageKmh;
        public final String typeName;
        public final boolean speeding;
        public Snapshot(GpsRadarPoint point, int distanceMeters,
                        int averageKmh, boolean speeding, String typeName) {
            this.point=point;this.distanceMeters=distanceMeters;
            speedLimitKmh=point==null?0:point.speedLimitKmh;
            this.averageKmh=averageKmh;
            this.speeding=speeding;this.typeName=typeName;
        }
    }
    private static volatile Snapshot current;
    public static Snapshot latest(){return current;}
    public static void publish(Snapshot snapshot){current=snapshot;}
    private GpsRadarState() {}
}
