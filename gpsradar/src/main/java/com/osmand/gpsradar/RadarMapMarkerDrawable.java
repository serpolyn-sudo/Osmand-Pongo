package com.osmand.gpsradar;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Compact native-style OsmAnd camera map icon; 24dp by default, 32 TYPE variants. */
public final class RadarMapMarkerDrawable extends Drawable {
    private final int type;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    public RadarMapMarkerDrawable(int type) {
        if (!RadarCamera.TYPES.containsKey(type)) throw new IllegalArgumentException();
        this.type=type;
    }
    @Override public void draw(@NonNull Canvas canvas) {
        Rect b=getBounds();
        float side=Math.min(b.width(),b.height());
        float x=b.left+(b.width()-side)/2f, y=b.top+(b.height()-side)/2f;
        canvas.save();
        canvas.translate(x,y);
        canvas.scale(side/24f,side/24f);
        boolean risk = type==40 || type==41 || type==51 || type==52 || type==63;
        boolean traffic = type==36 || type==43;
        boolean camera = (type>=32 && type<=37)||type==39||type==46||
                type==49||type==56||type==57||type==58||type==59||type==62;
        p.setStyle(Paint.Style.FILL);
        p.setColor(risk ? 0xFFF2AA21 : traffic ? 0xFFD04A42 : camera ? 0xFFE98E29 : 0xFF53677B);
        canvas.drawRoundRect(1,1,23,23,5,5,p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1);
        p.setColor(0xFF18222D);
        canvas.drawRoundRect(1,1,23,23,5,5,p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.WHITE);
        if (camera) {
            canvas.drawRoundRect(4,6,15,14,2,2,p);
            float cx=9.5f,cy=10;
            p.setColor(0xFF25313D); canvas.drawCircle(cx,cy,2.5f,p);
            p.setColor(Color.WHITE);canvas.drawRect(15,8,20,12,p);
        } else if (traffic) {
            canvas.drawRoundRect(8,3,16,17,2,2,p);
            p.setColor(0xFFD04A42);canvas.drawCircle(12,6,2,p);
            p.setColor(0xFFF3D557);canvas.drawCircle(12,10,2,p);
            p.setColor(0xFF30B47C);canvas.drawCircle(12,14,2,p);
        } else if (risk) {
            android.graphics.Path triangle=new android.graphics.Path();
            triangle.moveTo(12,3);triangle.lineTo(21,18);triangle.lineTo(3,18);triangle.close();
            canvas.drawPath(triangle,p);
            p.setColor(0xFF283441);
            p.setTextSize(10);p.setFakeBoldText(true);
            canvas.drawText("!",10.6f,16f,p);
        } else {
            p.setStrokeWidth(2);p.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(12,11,6,p);p.setStyle(Paint.Style.FILL);
            canvas.drawCircle(12,11,2,p);
        }
        // Small TYPE label distinguishes each category without enormous bitmap assets.
        p.setColor(0xFF101820);canvas.drawRoundRect(8,17,22,23,2,2,p);
        p.setColor(Color.WHITE);p.setTextSize(6.7f);p.setFakeBoldText(true);
        canvas.drawText(Integer.toString(type),10.5f,22f,p);
        canvas.restore();
    }
    @Override public void setAlpha(int alpha) { p.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(@Nullable android.graphics.ColorFilter filter) { p.setColorFilter(filter);invalidateSelf(); }
    @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
}
