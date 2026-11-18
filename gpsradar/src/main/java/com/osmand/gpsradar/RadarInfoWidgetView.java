package com.osmand.gpsradar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;

/**
 * Compact native-style content for a GPSRadar map widget.
 * Parent widget registration (side / top / bottom) must use OsmAnd WidgetType
 * and MapWidgetsFactory; this class provides the actual visible component.
 */
public final class RadarInfoWidgetView extends LinearLayout {
    private final ImageView pictogram;
    private final TextView title;
    private final TextView details;
    private final TextView distance;
    public RadarInfoWidgetView(Context context) { this(context,null); }
    public RadarInfoWidgetView(Context context,@Nullable AttributeSet attrs) {
        super(context,attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int pad=dp(7);
        setPadding(pad,dp(4),pad,dp(4));
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(0xED202225);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1),0xFF4D5158);
        setBackground(bg);
        pictogram=new ImageView(context);
        addView(pictogram,new LayoutParams(dp(23),dp(23)));
        LinearLayout textColumn=new LinearLayout(context);
        textColumn.setOrientation(VERTICAL);
        LayoutParams center=new LayoutParams(0,LayoutParams.WRAP_CONTENT,1);
        center.leftMargin=dp(6);
        addView(textColumn,center);
        title=new TextView(context);
        title.setTextSize(12);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        title.setMaxLines(2);
        textColumn.addView(title);
        details=new TextView(context);
        details.setTextSize(11);
        details.setTextColor(0xFFCFD1D4);
        textColumn.addView(details);
        distance=new TextView(context);
        distance.setTextSize(13);
        distance.setTextColor(0xFFF6F6F6);
        distance.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        addView(distance);
        setVisibility(GONE);
    }
    private int dp(int value) {
        return (int)(getResources().getDisplayMetrics().density*value+0.5f);
    }

    /** Null average means no validated average-speed interval is active. */
    public void show(RadarCamera object,int actualMeters,double currentSpeedKmh,
                     @Nullable Double measuredAverageKmh,int marginKmh) {
        setVisibility(VISIBLE);
        RadarMapMarkerDrawable marker=new RadarMapMarkerDrawable(object.type);
        marker.setBounds(0,0,dp(23),dp(23));
        pictogram.setImageDrawable(marker);
        title.setText(object.label());
        boolean exceeding=object.hasSpeedLimit() && Double.isFinite(currentSpeedKmh)
                && currentSpeedKmh > object.speedLimitKmh+marginKmh;
        boolean averageExceeding=object.isAverageSpeed() && measuredAverageKmh!=null
                && Double.isFinite(measuredAverageKmh) && object.hasSpeedLimit()
                && measuredAverageKmh > object.speedLimitKmh+marginKmh;
        String meta="";
        if (object.hasSpeedLimit()) meta="Лимит "+object.speedLimitKmh+" км/ч";
        if (object.isAverageSpeed() && measuredAverageKmh!=null
                && Double.isFinite(measuredAverageKmh)) {
            meta+=(meta.isEmpty()?"":"  •  ")+"Средняя "
                    +Math.round(measuredAverageKmh)+" км/ч";
        }
        details.setText(meta);
        details.setVisibility(meta.isEmpty()?GONE:VISIBLE);
        details.setTextColor(exceeding||averageExceeding?0xFFFF6767:0xFFCFD1D4);
        distance.setText(actualMeters<1000?actualMeters+" м":
                String.format(java.util.Locale.getDefault(),"%.1f км",actualMeters/1000.0));
        distance.setTextColor(exceeding||averageExceeding?0xFFFF6767:0xFFFFFFFF);
    }
    public void clear() { setVisibility(GONE); }
}
