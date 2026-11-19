package net.osmand.plus.gpsradar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<Integer,Bitmap> icons=new HashMap<>();
    private Bitmap originalIconAtlas;
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
            Bitmap bmp=icon(p.type);
            if(bmp!=null) {
                float w=bmp.getWidth(),h=bmp.getHeight();
                float factor=Math.min(size/w,size/h);
                float halfW=w*factor/2f,halfH=h*factor/2f;
                canvas.drawBitmap(bmp,null,new RectF(x-halfW,y-halfH,x+halfW,y+halfH),paint);
            } else {
                paint.setColor(0xFFE39A35);
                canvas.drawRoundRect(x-size/2f,y-size/2f,x+size/2f,y+size/2f,
                      size/5f,size/5f,paint);
                paint.setColor(0xFF111820);
                paint.setTextSize(size*.42f);
                canvas.drawText(Integer.toString(p.type),x-size*.3f,y+size*.14f,paint);
            }
        }
    }
    private Bitmap icon(int type) {
        if(icons.containsKey(type))return icons.get(type);
        Bitmap bitmap=null;
        try {
            if(originalIconAtlas==null) {
                try(java.io.InputStream input=app.getAssets().open(
                        "gpsradar_icons/atlas24.png")) {
                    originalIconAtlas=BitmapFactory.decodeStream(input);
                }
            }
            if(originalIconAtlas!=null && originalIconAtlas.getWidth()==192 &&
                    originalIconAtlas.getHeight()==96 && type>=32 && type<=63) {
                int index=type-32;
                bitmap=Bitmap.createBitmap(originalIconAtlas,
                        (index%8)*24,(index/8)*24,24,24);
            }
        } catch(Exception e) {
            android.util.Log.w("GPSRadar","Cannot read original TYPE "+type+" icon",e);
        }
        icons.put(type,bitmap);
        return bitmap;
    }
}
