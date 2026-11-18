package net.osmand.plus.views.mapwidgets.widgets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.gpsradar.GpsRadarState;
import net.osmand.plus.views.mapwidgets.WidgetType;

/** Registered with the original OsmAnd widget catalog (left/right/top/bottom). */
public class GpsRadarAlertWidget extends SimpleWidget {
    private long updated;
    public GpsRadarAlertWidget(@NonNull MapActivity activity, @Nullable String customId,
                               @Nullable WidgetsPanel panel) {
        super(activity, WidgetType.GPSRADAR_ALERT, customId, panel);
        setIcons(WidgetType.GPSRADAR_ALERT);
    }

    @Override protected void updateSimpleWidgetInfo(@Nullable DrawSettings settings) {
        long time=android.os.SystemClock.elapsedRealtime();
        if (!isUpdateNeeded() && time-updated<800) return;
        updated=time;
        GpsRadarState.Snapshot s=GpsRadarState.latest();
        if(s==null || s.point==null) {
            setText(NO_VALUE, "GPSRadar");
            return;
        }
        String primary=s.distanceMeters<1000?s.distanceMeters+" м":
                String.format(java.util.Locale.getDefault(),"%.1f км",s.distanceMeters/1000d);
        String secondary=s.typeName;
        if (s.speedLimitKmh>0) secondary+=" · "+s.speedLimitKmh+" км/ч";
        if (s.averageKmh>=0) secondary+=" · средняя "+s.averageKmh+" км/ч";
        if (s.speeding) secondary+=" · ПРЕВЫШЕНИЕ";
        setText(primary,secondary);
    }
}
