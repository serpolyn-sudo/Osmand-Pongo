package net.osmand.plus.gpsradar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.util.HashMap;
import java.util.Map;

/** Blue circular POI-style markers for all GPSRadar TYPE 32–63 categories. */
public final class GpsRadarIconProvider {
    private static final Map<Integer,Bitmap> CACHE=new HashMap<>();
    private GpsRadarIconProvider() {}

    public static synchronized Drawable drawable(Context context,int type) {
        if(type<32||type>63)return null;
        Bitmap bitmap=CACHE.get(type);
        if(bitmap==null) {
            bitmap=render(type);
            CACHE.put(type,bitmap);
        }
        return new BitmapDrawable(context.getResources(),bitmap);
    }
    private static Bitmap render(int type) {
        Bitmap b=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.rgb(3,152,219));
        c.drawCircle(48,48,44,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2f);
        p.setColor(Color.rgb(74,206,247));c.drawCircle(48,48,43,p);
        p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);
        switch(type) {
            case 35: // average speed, two cameras
                camera(c,p,31,38,0.68f);camera(c,p,59,55,0.68f);break;
            case 36: case 43: // traffic light
                camera(c,p,34,50,0.8f);
                rounded(c,p,61,23,76,72,5);p.setColor(0xff0398db);
                c.drawCircle(68,34,4,p);c.drawCircle(68,47,4,p);c.drawCircle(68,60,4,p);
                break;
            case 49: case 57: // mobile tripod
                rounded(c,p,26,26,68,47,7);p.setColor(0xff0398db);
                c.drawCircle(44,36,7,p);p.setColor(Color.WHITE);
                line(c,p,47,48,47,59,4);line(c,p,47,59,25,76,4);line(c,p,47,59,70,76,4);
                break;
            case 39: // bus
                rounded(c,p,25,25,71,70,8);p.setColor(0xff0398db);
                rounded(c,p,31,32,65,51,3);p.setColor(Color.WHITE);
                c.drawCircle(34,66,4,p);c.drawCircle(62,66,4,p);break;
            case 56: // no stopping
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(8);
                c.drawCircle(48,48,25,p);line(c,p,31,31,65,65,7);
                line(c,p,65,31,31,65,7);break;
            case 61: case 41: // pedestrian/school
                c.drawCircle(48,29,7,p);line(c,p,48,38,48,59,6);
                line(c,p,48,44,31,54,5);line(c,p,48,44,64,50,5);
                line(c,p,48,57,35,75,6);line(c,p,48,57,62,75,6);break;
            case 38: // railway crossing
                rounded(c,p,29,20,67,61,7);p.setColor(0xff0398db);
                rounded(c,p,35,27,61,45,2);p.setColor(Color.WHITE);
                line(c,p,22,76,74,76,4);line(c,p,27,68,69,68,4);break;
            case 40: case 52: case 63: // warning
                Path triangle=new Path();triangle.moveTo(48,19);triangle.lineTo(78,74);
                triangle.lineTo(18,74);triangle.close();
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);c.drawPath(triangle,p);
                p.setStyle(Paint.Style.FILL);rounded(c,p,45,36,51,58,2);
                c.drawCircle(48,66,4,p);break;
            case 51: case 50: // rough road / speed bump
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(8);
                Path road=new Path();road.moveTo(17,62);road.lineTo(29,62);
                road.quadTo(37,31,46,52);road.quadTo(54,72,65,47);
                road.lineTo(79,62);c.drawPath(road,p);break;
            case 55: // police car
                rounded(c,p,19,42,77,65,9);rounded(c,p,29,32,67,48,5);
                c.drawCircle(30,66,5,p);c.drawCircle(66,66,5,p);
                rounded(c,p,43,24,53,33,3);break;
            case 44: case 47: // toll barrier
                rounded(c,p,23,29,39,71,3);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6);
                line(c,p,39,44,77,34,6);break;
            case 54: // POI pin
                c.drawCircle(48,39,22,p);
                Path pin=new Path();pin.moveTo(29,48);pin.lineTo(48,77);
                pin.lineTo(67,48);pin.close();c.drawPath(pin,p);
                p.setColor(0xff0398db);c.drawCircle(48,39,9,p);break;
            case 42: case 45: // town signs
                rounded(c,p,27,29,69,62,3);p.setColor(0xff0398db);
                rounded(c,p,35,37,61,43,2);rounded(c,p,35,49,61,55,2);break;
            case 48: // give way
                Path yield=new Path();yield.moveTo(20,28);yield.lineTo(76,28);
                yield.lineTo(48,73);yield.close();
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);c.drawPath(yield,p);break;
            case 53: // tunnel
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(9);
                c.drawArc(23,20,73,83,180,180,false,p);line(c,p,23,52,23,77,8);
                line(c,p,73,52,73,77,8);break;
            case 60: // highway exit
                line(c,p,31,22,31,73,8);line(c,p,58,22,58,49,8);
                line(c,p,58,49,77,72,7);break;
            case 34: case 37: case 58: case 59: // camera directed to rear
            case 46: 
                camera(c,p,48,44,1f);line(c,p,28,72,68,72,5);break;
            default:
                camera(c,p,48,48,1f);break;
        }
        return b;
    }
    private static void camera(Canvas c,Paint p,float cx,float cy,float scale) {
        c.save();c.translate(cx,cy);c.scale(scale,scale);
        p.setColor(Color.WHITE);
        rounded(c,p,-27,-18,17,18,7);
        rounded(c,p,15,-10,29,11,4);
        p.setColor(0xff0398db);c.drawCircle(-8,0,10,p);
        p.setColor(Color.WHITE);c.restore();
    }
    private static void line(Canvas c,Paint p,float x1,float y1,float x2,float y2,float width) {
        Paint.Style old=p.getStyle();float prev=p.getStrokeWidth();
        p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(width);c.drawLine(x1,y1,x2,y2,p);
        p.setStyle(old);p.setStrokeWidth(prev);
    }
    private static void rounded(Canvas c,Paint p,float l,float t,float r,float b,float radius) {
        c.drawRoundRect(l,t,r,b,radius,radius,p);
    }
}
