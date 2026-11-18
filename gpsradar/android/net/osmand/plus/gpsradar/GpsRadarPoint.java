package net.osmand.plus.gpsradar;

/** iGO camera record with START / END type 35 markers kept intact. */
public final class GpsRadarPoint {
    public final double latitude, longitude, bearingDegrees;
    public final int type, speedLimitKmh, directionType;
    public final String country;
    public GpsRadarPoint(double lat, double lon, double bearing, int type, int limit,
                         int directionType, String country) {
        latitude=lat;longitude=lon; bearingDegrees=bearing;
        this.type=type;speedLimitKmh=limit;this.directionType=directionType;
        this.country=country;
    }
}
