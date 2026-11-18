package com.osmand.gpsradar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Uses actual GPS distances for UI and voice, never an artificial trigger label. */
public final class RadarWarningEngine {
    private final Set<String> announced = new HashSet<>();

    public static final class Warning {
        public final RadarCamera camera;
        public final int actualDistanceMeters;
        public final boolean exceedsLimit;
        public final String speech;
        private Warning(RadarCamera c, int d, boolean over, String speech) {
            camera=c; actualDistanceMeters=d; exceedsLimit=over; this.speech=speech;
        }
    }

    public List<Warning> check(double lat, double lon, double heading, double gpsSpeedKmh,
                               List<RadarCamera> candidates, RadarProfileSettings.Profile settings) {
        List<Warning> result = new ArrayList<>();
        if (!settings.enabled || candidates == null) return result;
        for (RadarCamera c: candidates) {
            if (!settings.typeEnabled[c.type]) continue;
            int distance = (int)Math.round(distanceMeters(lat,lon,c.latitude,c.longitude));
            if (distance > 3000) continue;
            if (Double.isFinite(heading) &&
                angleDifference(heading,bearing(lat,lon,c.latitude,c.longitude)) > 65) continue;
            if (c.directionType != 0 && Double.isFinite(c.bearingDegrees) &&
                Double.isFinite(heading) && angleDifference(heading,c.bearingDegrees) > 55) continue;
            boolean over = c.speedLimitKmh > 0 && Double.isFinite(gpsSpeedKmh) &&
                    gpsSpeedKmh > c.speedLimitKmh + settings.speedToleranceKmh;
            for (int trigger : settings.distancesMeters) {
                if (distance > trigger || distance < trigger - 150) continue;
                String key=c.country+":"+c.latitude+":"+c.longitude+":"+c.type+":"+trigger;
                if (!announced.add(key)) continue;
                String speech="Впереди "+c.label().toLowerCase(Locale.ROOT)+", "+distance+" метров";
                if (c.hasSpeedLimit()) speech+=", ограничение "+c.speedLimitKmh+" километров в час";
                if (over) speech+=", превышение скорости";
                result.add(new Warning(c,distance,over,speech));
            }
        }
        return result;
    }

    public void resetOnNewRoute() { announced.clear(); }

    public static double distanceMeters(double lat1,double lon1,double lat2,double lon2) {
        double a=Math.toRadians(lat1), b=Math.toRadians(lat2);
        double x=b-a,y=Math.toRadians(lon2-lon1);
        double q=Math.pow(Math.sin(x/2),2)+Math.cos(a)*Math.cos(b)*Math.pow(Math.sin(y/2),2);
        return 12742000*Math.atan2(Math.sqrt(q),Math.sqrt(Math.max(0,1-q)));
    }
    private static double bearing(double lat1,double lon1,double lat2,double lon2) {
        double a=Math.toRadians(lat1),b=Math.toRadians(lat2),d=Math.toRadians(lon2-lon1);
        return (Math.toDegrees(Math.atan2(Math.sin(d)*Math.cos(b),
                Math.cos(a)*Math.sin(b)-Math.sin(a)*Math.cos(b)*Math.cos(d)))+360)%360;
    }
    private static double angleDifference(double a,double b) {
        double d=Math.abs((a-b+360)%360);
        return Math.min(d,360-d);
    }
}
