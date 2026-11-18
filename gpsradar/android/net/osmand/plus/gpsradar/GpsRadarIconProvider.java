package net.osmand.plus.gpsradar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Original GPSRadar icons, proportionally downscaled, never cropped. */
public final class GpsRadarIconProvider {
    private static Bitmap atlas;
    private static final Map<Integer,Bitmap> CACHE=new HashMap<>();
    private GpsRadarIconProvider(){}

    public static synchronized Drawable drawable(Context context,int type) {
        if(type<32||type>63)return null;
        try {
            if(atlas==null) {
                try(InputStream in=context.getAssets().open("gpsradar_icons/atlas24.png")) {
                    atlas=BitmapFactory.decodeStream(in);
                }
            }
            if(atlas==null || atlas.getWidth()!=192 || atlas.getHeight()!=96)return null;
            Bitmap icon=CACHE.get(type);
            if(icon==null) {
                int i=type-32;
                icon=Bitmap.createBitmap(atlas,(i%8)*24,(i/8)*24,24,24);
                CACHE.put(type,icon);
            }
            return new BitmapDrawable(context.getResources(),icon);
        } catch(Exception ignored) {
            return null;
        }
    }
}
