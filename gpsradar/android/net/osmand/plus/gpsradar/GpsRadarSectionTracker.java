package net.osmand.plus.gpsradar;

import net.osmand.Location;
import java.util.List;

/**
 * Section average from FIRST validated TYPE=35 entry to TYPE=35 SPEED=5 exit.
 * The regular OsmAnd AverageSpeedWidget computes a rolling interval and is
 * deliberately NOT used as an average-speed-camera section measurement.
 */
public final class GpsRadarSectionTracker {
    public static final class Reading {
        public final boolean active, completed;
        public final double averageKmh;
        public final int speedLimitKmh;
        public final int traveledMeters;
        Reading(boolean active, boolean completed, double avg, int limit, int distance) {
            this.active=active;this.completed=completed;averageKmh=avg;
            speedLimitKmh=limit;traveledMeters=distance;
        }
    }
    private boolean active;
    private long startNanos, lastNanos, lastStartTime;
    private double travelled;
    private Location previous;
    private int limit;
    private double startLat,startLon;
    private int directionBucket;

    private static double angleDiff(double a,double b) {
        double v=Math.abs(((a-b)%360+360)%360);
        return Math.min(v,360-v);
    }

    public void reset() { active=false; previous=null; travelled=0;startNanos=0; }

    public Reading update(Location position, List<GpsRadarPoint> nearby) {
        if (position==null || !position.hasAccuracy() || position.getAccuracy()>65f
                || !position.hasSpeed() || position.getSpeed()<1.4f) {
            return snapshot(false);
        }
        long time=android.os.SystemClock.elapsedRealtimeNanos();
        if(time<=0) return snapshot(false);
        if(active && previous!=null && lastNanos>0) {
            long dt=time-lastNanos;
            float step=previous.distanceTo(position);
            if(dt>0 && dt<15_000_000_000L
                    && step>=0 && step<Math.max(70, dt/1_000_000_000d*60)) {
                travelled+=step;
            }
            if(time-startNanos>30L*60*1_000_000_000 || travelled>35_000) reset();
        }
        previous=new Location(position);
        lastNanos=time;

        GpsRadarPoint start=null,end=null;
        float startMeters=Float.MAX_VALUE,endMeters=Float.MAX_VALUE;
        if(nearby!=null) for(GpsRadarPoint p:nearby) {
            if(p.type!=35)continue;
            if(p.directionType==1 && position.hasBearing()
                    && angleDiff(position.getBearing(),p.bearingDegrees)>40) continue;
            Location loc=new Location("gpsradar-point");
            loc.setLatitude(p.latitude);loc.setLongitude(p.longitude);
            float meters=position.distanceTo(loc);
            if(p.speedLimitKmh==5) {
                if(meters<endMeters){end=p;endMeters=meters;}
            } else if(p.speedLimitKmh>=20 && p.speedLimitKmh<=180) {
                if(meters<startMeters){start=p;startMeters=meters;}
            }
        }
        if(!active && start!=null && startMeters<=35f
                && (position.getTime()-lastStartTime>120_000
                  || Math.abs(start.latitude-startLat)>0.00001
                  || Math.abs(start.longitude-startLon)>0.00001)) {
            active=true;
            startNanos=time; travelled=0; limit=start.speedLimitKmh;
            startLat=start.latitude;startLon=start.longitude;
            lastStartTime=position.getTime();
            return snapshot(false);
        }
        if(active && end!=null && endMeters<=35 && travelled>250
                && time-startNanos>10_000_000_000L) {
            Reading endReading=snapshot(true);
            reset();
            return endReading;
        }
        return snapshot(false);
    }
    private Reading snapshot(boolean completed) {
        if(!active) return new Reading(false,false,Double.NaN,0,0);
        long secondsNanos=lastNanos-startNanos;
        double speed=secondsNanos>0?travelled*3.6/(secondsNanos/1e9):Double.NaN;
        return new Reading(!completed,completed,speed,limit,(int)Math.round(travelled));
    }
}
