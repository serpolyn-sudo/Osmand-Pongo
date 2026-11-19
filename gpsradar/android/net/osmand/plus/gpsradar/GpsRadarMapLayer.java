package net.osmand.plus.gpsradar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Map layer independent from turn-by-turn navigation. The map displays markers
 * based on screen center/zoom, not current GPS position.
 */
public final class GpsRadarMapLayer extends OsmandMapLayer {
    private final OsmandApplication app;
    private final GpsRadarStore store;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private volatile List<GpsRadarPoint> visible=java.util.Collections.emptyList();
    private volatile boolean queryRunning=false;
    private volatile long lastQuery=0;
    private double queryLat=999,queryLon=999;
    private int queryZoom=-1;
    private String queryVehicle="";

    public GpsRadarMapLayer(@NonNull OsmandApplication app) {
        super(app);this.app=app;store=new GpsRadarStore(app);
        GpsRadarEngine.initialize(app);
    }
    @Override public boolean drawInScreenPixels() { return false; }

    @Override public void onDraw(Canvas canvas,RotatedTileBox box,DrawSettings settings) {
        String profile=app.getSettings().getApplicationMode().getStringKey();
        android.content.SharedPreferences pref=app.getSharedPreferences(
                "gpsradar_profile_"+profile,Context.MODE_PRIVATE);
        if(!pref.getBoolean("markers",true))return;
        String vehicle=app.getSettings().getApplicationMode().isDerivedRoutingFrom(ApplicationMode.TRUCK)?"truck":"car";
        double lat=box.getLatitude(),lon=box.getLongitude();
        int zoom=box.getZoom();
        long now=android.os.SystemClock.elapsedRealtime();
        if(!queryRunning && (zoom!=queryZoom||!vehicle.equals(queryVehicle)||
                Math.abs(lat-queryLat)>0.005||Math.abs(lon-queryLon)>0.005)
                && now-lastQuery>250) {
            queryRunning=true; queryLat=lat;queryLon=lon;queryZoom=zoom;
            queryVehicle=vehicle;lastQuery=now;
            int radius=(int)Math.min(100000,Math.max(2500,
                    3500*Math.pow(2,14-zoom)));
            worker.execute(()->{
                try {visible=store.near(vehicle,lat,lon,radius);}
                catch(Exception ex){android.util.Log.w("GPSRadar","Map query failed",ex);}
                finally {queryRunning=false;
                    app.runInUIThread(()->getMapView().refreshMap());}
            });
        }
        if(zoom<9)return; // too many overlapping objects at continent scale
        int size=Math.max(14,Math.round(24*box.getDensity()));
        int drawn=0;
        for(GpsRadarPoint p:visible) {
            if(!pref.getBoolean("type_"+p.type,true))continue;
            int x=box.getPixXFromLonNoRot(p.longitude);
            int y=box.getPixYFromLatNoRot(p.latitude);
            if(x<-size||y<-size||x>box.getPixWidth()+size||y>box.getPixHeight()+size)continue;
            if(++drawn>850)break;
            Drawable marker=GpsRadarIconProvider.drawable(app,p.type);
            if(marker!=null) {
                marker.setBounds(x-size/2,y-size/2,x+size/2,y+size/2);
                marker.draw(canvas);
            }
        }
    }
}
