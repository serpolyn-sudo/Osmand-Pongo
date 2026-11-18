package net.osmand.plus.views.controls;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.routing.NextDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.router.TurnType;

import java.util.Locale;

/** Portrait Pongo navigation guidance using OsmAnd route and GPS state. */
public class PongoTopHud extends LinearLayout {

    private final OsmandApplication app;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LinearLayout turns;
    private final TextView first;
    private final TextView second;
    private final TextView clock;
    private final TextView elevation;
    private final TextView settings;
    private final Runnable update = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, 1500);
        }
    };

    public PongoTopHud(Context context) {
        this(context, null);
    }

    public PongoTopHud(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        app = (OsmandApplication) context.getApplicationContext();
        setOrientation(HORIZONTAL);
        setGravity(Gravity.TOP);
        setPadding(dp(4), dp(4), dp(4), 0);
        setClickable(false);

        turns = new LinearLayout(context);
        turns.setOrientation(HORIZONTAL);
        turns.setGravity(Gravity.CENTER_VERTICAL);
        turns.setBackground(panel());
        LinearLayout.LayoutParams turnParams = new LinearLayout.LayoutParams(0, dp(97), 1);
        turnParams.rightMargin = dp(5);
        addView(turns, turnParams);

        first = text(24, Typeface.BOLD);
        first.setGravity(Gravity.CENTER);
        second = text(14, Typeface.BOLD);
        second.setGravity(Gravity.CENTER);
        turns.addView(first, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 2));
        turns.addView(second, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1));

        LinearLayout status = new LinearLayout(context);
        status.setOrientation(VERTICAL);
        status.setGravity(Gravity.CENTER);
        status.setBackground(panel());
        addView(status, new LinearLayout.LayoutParams(dp(106), dp(97)));

        clock = text(23, Typeface.BOLD);
        elevation = text(13, Typeface.NORMAL);
        settings = text(19, Typeface.BOLD);
        settings.setText("☰");
        settings.setContentDescription("Быстрые настройки карты");
        settings.setClickable(true);
        settings.setFocusable(true);
        settings.setOnClickListener(v -> {
            View nativeSettings = getRootView().findViewById(R.id.map_layers_button);
            if (nativeSettings != null) nativeSettings.performClick();
        });

        status.addView(clock, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(35)));
        status.addView(settings, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(26)));
        status.addView(elevation, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(28)));
        refresh();
    }

    private int dp(float d) {
        return (int) (d * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(int size, int weight) {
        TextView v = new TextView(getContext());
        v.setTextColor(Color.WHITE);
        v.setTextSize(size);
        v.setTypeface(Typeface.DEFAULT, weight);
        v.setIncludeFontPadding(false);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private GradientDrawable panel() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(0xF0000000);
        drawable.setCornerRadius(dp(12));
        drawable.setStroke(dp(1), 0xFFCCCCCC);
        return drawable;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.removeCallbacks(update);
        update.run();
    }

    @Override protected void onDetachedFromWindow() {
        handler.removeCallbacks(update);
        super.onDetachedFromWindow();
    }

    private String direction(NextDirectionInfo info) {
        if (info == null || info.directionInfo == null ||
            info.directionInfo.getTurnType() == null) return "↑";
        switch (info.directionInfo.getTurnType().getValue()) {
            case TurnType.TL: case TurnType.TSHL: return "←";
            case TurnType.TSLL: case TurnType.KL: return "↖";
            case TurnType.TR: case TurnType.TSHR: return "→";
            case TurnType.TSLR: case TurnType.KR: return "↗";
            case TurnType.TU: case TurnType.TRU: return "↶";
            case TurnType.RNDB: case TurnType.RNLB: return "↻";
            default: return "↑";
        }
    }

    private String distance(int meters) {
        if (meters < 1000) return Math.max(meters, 0) + " м";
        if (meters < 10000) return String.format(Locale.getDefault(), "%.1f км", meters / 1000.0);
        return String.format(Locale.getDefault(), "%.0f км", meters / 1000.0);
    }

    private void refresh() {
        clock.setText(DateFormat.format("HH:mm", System.currentTimeMillis()));
        Location location = app.getLocationProvider().getLastKnownLocation();
        elevation.setText(location != null && location.hasAltitude()
                ? Math.round(location.getAltitude()) + " м" : "— м");

        RoutingHelper routing = app.getRoutingHelper();
        boolean active = routing != null && routing.isFollowingMode()
                && routing.isRouteCalculated();
        turns.setVisibility(active ? VISIBLE : INVISIBLE);
        if (!active) return;

        NextDirectionInfo next = routing.getNextRouteDirectionInfo(new NextDirectionInfo(), false);
        if (next == null || next.directionInfo == null) {
            first.setText("↑");
            second.setText("");
            return;
        }
        first.setText(direction(next) + "\n" + distance(next.distanceTo));
        NextDirectionInfo after = routing.getNextRouteDirectionInfoAfter(
                next, new NextDirectionInfo(), false);
        second.setText(after != null && after.directionInfo != null
                ? direction(after) + "\n" + distance(after.distanceTo) : "");
    }
}
