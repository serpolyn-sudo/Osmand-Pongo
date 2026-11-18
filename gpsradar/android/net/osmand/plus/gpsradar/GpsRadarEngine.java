package net.osmand.plus.gpsradar;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;

import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmAndLocationProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Uses original OsmAnd GPS provider, not a second Android location service. */
public final class GpsRadarEngine implements OsmAndLocationProvider.OsmAndLocationListener {
    private static volatile GpsRadarEngine current;
    private final OsmandApplication app;
    private final GpsRadarStore store;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy=new AtomicBoolean();
    private final GpsRadarSectionTracker sections=new GpsRadarSectionTracker();
    private final Set<String> alerted=new HashSet<>();
    private TextToSpeech speech;
    private boolean speechReady;
    private String lastProfile="";
    private volatile List<GpsRadarPoint> cachedPoints=java.util.Collections.emptyList();

    private GpsRadarEngine(OsmandApplication app) {
        this.app=app;
        store=new GpsRadarStore(app);
        speech=new TextToSpeech(app,status->{
            speechReady=status==TextToSpeech.SUCCESS;
            if(speechReady && speech!=null)speech.setLanguage(new Locale("ru","RU"));
        });
        app.getLocationProvider().addLocationListener(this);
    }
    public static synchronized void initialize(OsmandApplication app) {
        if(current==null)current=new GpsRadarEngine(app);
    }
    public static GpsRadarEngine instance(){return current;}
    public List<GpsRadarPoint> lastNearby(){return cachedPoints;}

    @Override public void updateLocation(Location loc) {
        if(loc==null || !loc.hasAccuracy() || loc.getAccuracy()>60f) {
            GpsRadarState.publish(null);return;
        }
        if(!busy.compareAndSet(false,true))return;
        Location fix=new Location(loc);
        worker.execute(()->{
            try { evaluate(fix); }
            catch(Exception ex) {
                android.util.Log.w("GpsRadar","GPSRadar update failed",ex);
            } finally {busy.set(false);}
        });
    }
    private void evaluate(Location fix) {
        String profile=app.getSettings().getApplicationMode().getStringKey();
        if(!profile.equals(lastProfile)) {
            lastProfile=profile; alerted.clear(); sections.reset();
        }
        String vehicle="truck".equalsIgnoreCase(profile)?"truck":"car";
        SharedPreferences pref=app.getSharedPreferences("gpsradar_profile_"+profile,
                                                        Context.MODE_PRIVATE);
        boolean extended=pref.getBoolean("extended",false);
        boolean show=pref.getBoolean("markers",true);
        int tolerance=pref.getInt("tolerance",0);
        List<GpsRadarPoint> nearby=store.near(vehicle,fix.getLatitude(),fix.getLongitude(),3200);
        cachedPoints=nearby;
        int[] trigger=readThresholds(pref.getString("thresholds","700,300,150"));
        GpsRadarSectionTracker.Reading avg=sections.update(fix,nearby);
        GpsRadarPoint closest=null;
        int distance=Integer.MAX_VALUE;
        double course=fix.hasBearing()?fix.getBearing():Double.NaN;
        for(GpsRadarPoint p:nearby) {
            if(p.type==35 && p.speedLimitKmh==5)continue; // section end only
            if(!pref.getBoolean("type_"+p.type,true))continue;
            double meters=GpsRadarSectionTrackerDistance.distance(fix,p);
            if(meters>3000)continue;
            if(Double.isFinite(course) && angleDiff(course,
                    GpsRadarSectionTrackerDistance.bearing(fix,p))>65)continue;
            if(p.directionType==1 && Double.isFinite(course) &&
                    angleDiff(course,p.bearingDegrees)>40)continue;
            if(meters<distance) { closest=p;distance=(int)Math.round(meters); }
        }
        if(closest==null) { GpsRadarState.publish(null);return; }
        int lim=closest.speedLimitKmh==5 && closest.type==35?0:closest.speedLimitKmh;
        boolean over=lim>0 && fix.hasSpeed() && fix.getSpeed()*3.6>lim+tolerance;
        int average=closest.type==35 && avg.active && Double.isFinite(avg.averageKmh)?
                (int)Math.round(avg.averageKmh):-1;
        GpsRadarState.publish(new GpsRadarState.Snapshot(closest,distance,average,over,
                cameraLabel(closest.type)));
        if(!extended)return; // stock OsmAnd warning behavior remains unchanged
        for(int t:trigger) {
            String id=vehicle+":"+closest.latitude+":"+closest.longitude+":"+closest.type+":"+t;
            if(distance<=t && distance>=t-120 && alerted.add(id)) {
                String msg="Впереди "+cameraLabel(closest.type)+", "+distance+" метров";
                if(lim>0)msg+=", ограничение "+lim+" километров в час";
                if(over)msg+=", превышение скорости";
                speak(msg);
            }
        }
        // Only a TYPE 35 start with a real speed limit opens a section,
        // and only a TYPE 35 SPEED=5 end closes it.
        if(!avg.active && avg.completed) {
            speak("Конец участка контроля средней скорости, средняя "
                +Math.round(avg.averageKmh)+" километров в час");
        }
    }
    private void speak(String msg) {
        if(speechReady && speech!=null)
            speech.speak(msg,TextToSpeech.QUEUE_ADD,null,"gpsradar"+System.nanoTime());
    }
    private static double angleDiff(double a,double b) {
        double d=Math.abs(((a-b)%360+360)%360); return Math.min(d,360-d);
    }
    public static String cameraLabel(int type) {
        String[] names={
          "камера контроля скорости","возможно засада",
          "встроенная камера контроля скорости","камера контроля средней скорости",
          "контроль проезда на красный","фото радарный комплекс",
          "железнодорожный переезд","камера на полосе общественного транспорта",
          "аварийно-опасный участок","школьная территория",
          "начало населенного пункта","камера встроенная в светофор",
          "пункт оплаты","конец населенного пункта",
          "камера направлена в спину","контроль оплаты",
          "знак уступи дорогу","возможно тренога",
          "лежачий полицейский","участок плохой дороги",
          "опасный поворот","тоннель","объект ПОИ","пост ДПС",
          "камера контроля остановки","возможно тренога в спину",
          "фото радарный комплекс в спину","камера контроля скорости в спину",
          "съезд с автострады","пешеходный переход",
          "камера наблюдения","опасный участок дороги"
        };
        return type>=32 && type<=63?names[type-32]:"дорожный объект";
    }
    private static int[] readThresholds(String text) {
        if(text==null||text.isEmpty())return new int[]{700,300,150};
        try {
            String[] parts=text.split(",");
            if(parts.length<1||parts.length>8)throw new NumberFormatException();
            int[] result=new int[parts.length];
            for(int i=0;i<parts.length;i++) {
                result[i]=Integer.parseInt(parts[i].trim());
                if(result[i]<10||result[i]>3000)throw new NumberFormatException();
            }
            return result;
        } catch(NumberFormatException e) {
            return new int[]{700,300,150};
        }
    }
    private static final class GpsRadarSectionTrackerDistance {
        static double distance(Location fix,GpsRadarPoint p) {
            Location target=new Location("gpsradar");
            target.setLatitude(p.latitude);target.setLongitude(p.longitude);
            return fix.distanceTo(target);
        }
        static double bearing(Location fix,GpsRadarPoint p) {
            Location target=new Location("gpsradar");
            target.setLatitude(p.latitude);target.setLongitude(p.longitude);
            return fix.bearingTo(target);
        }
    }
}
