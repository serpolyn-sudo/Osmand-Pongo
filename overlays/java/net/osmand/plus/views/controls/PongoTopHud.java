package net.osmand.plus.views.controls;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.routing.CurrentStreetName;
import net.osmand.plus.routing.NextDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.mapwidgets.TurnDrawable;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * PORTRAIT Pongo HUD: the large road-shaped maneuver icon is rendered by
 * OsmAnd's own TurnDrawable (not text arrows). Replaces the native portrait
 * navigation panels while following a route, and restores them afterwards.
 * Landscape layouts do not contain this widget.
 */
public class PongoTopHud extends LinearLayout {
    private final OsmandApplication app;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<View, Integer> nativeVisibility = new IdentityHashMap<>();
    private final LinearLayout routeHeader;
    private final LinearLayout turns;
    private final ImageView primaryIcon;
    private final ImageView secondaryIcon;
    private final TurnDrawable primaryDrawable;
    private final TurnDrawable secondaryDrawable;
    private final TextView primaryDistance;
    private final TextView secondaryDistance;
    private final TextView routeText;
    private final TextView clock;
    private final TextView elevation;
    private final TextView settings;
    private final Runnable update = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, 1000);
        }
    };

    public PongoTopHud(Context context) {
        this(context, null);
    }

    public PongoTopHud(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        app = (OsmandApplication) context.getApplicationContext();
        setOrientation(VERTICAL);
        setPadding(dp(2), dp(2), dp(2), 0);
        setClipChildren(false);
        setClickable(false);

        // Upper road banner - only shown when a real road name is available.
        routeHeader = new LinearLayout(context);
        routeHeader.setGravity(Gravity.CENTER);
        routeHeader.setBackground(panel(13));
        LinearLayout.LayoutParams headerParams =
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(45));
        headerParams.bottomMargin = dp(2);
        addView(routeHeader, headerParams);
        routeText = label(17, Typeface.BOLD);
        routeText.setSingleLine(true);
        routeText.setEllipsize(TextUtils.TruncateAt.END);
        routeText.setPadding(dp(10), 0, dp(10), 0);
        routeHeader.addView(routeText, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.TOP);
        addView(row, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(116)));

        turns = new LinearLayout(context);
        turns.setOrientation(HORIZONTAL);
        turns.setGravity(Gravity.TOP);
        turns.setBackground(panel(23));
        LinearLayout.LayoutParams turnParams =
                new LinearLayout.LayoutParams(dp(178), dp(116));
        turnParams.rightMargin = dp(3);
        row.addView(turns, turnParams);

        LinearLayout firstColumn = new LinearLayout(context);
        firstColumn.setOrientation(VERTICAL);
        firstColumn.setGravity(Gravity.CENTER);
        turns.addView(firstColumn, new LinearLayout.LayoutParams(0,
                LayoutParams.MATCH_PARENT, 1f));

        primaryDrawable = new TurnDrawable(context, false);
        primaryDrawable.setBounds(0, 0, 72, 72);
        primaryDrawable.setRouteDirectionColor(android.R.color.white);
        primaryIcon = new ImageView(context);
        primaryIcon.setImageDrawable(primaryDrawable);
        primaryIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        firstColumn.addView(primaryIcon, new LinearLayout.LayoutParams(
                dp(78), dp(74)));

        primaryDistance = label(22, Typeface.BOLD);
        firstColumn.addView(primaryDistance,
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(34)));

        LinearLayout secondColumn = new LinearLayout(context);
        secondColumn.setOrientation(VERTICAL);
        secondColumn.setGravity(Gravity.CENTER);
        GradientDrawable miniBackground = panel(10);
        miniBackground.setStroke(dp(1), 0xFFEEEEEE);
        secondColumn.setBackground(miniBackground);
        LinearLayout.LayoutParams miniParams =
                new LinearLayout.LayoutParams(dp(65), dp(88));
        miniParams.topMargin = dp(4);
        miniParams.rightMargin = dp(4);
        turns.addView(secondColumn, miniParams);

        secondaryDrawable = new TurnDrawable(context, true);
        secondaryDrawable.setBounds(0, 0, 72, 72);
        secondaryDrawable.setRouteDirectionColor(android.R.color.white);
        secondaryIcon = new ImageView(context);
        secondaryIcon.setImageDrawable(secondaryDrawable);
        secondaryIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        secondColumn.addView(secondaryIcon, new LinearLayout.LayoutParams(
                dp(47), dp(51)));

        secondaryDistance = label(14, Typeface.BOLD);
        secondaryDistance.setSingleLine(true);
        secondColumn.addView(secondaryDistance,
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(29)));

        // Keep the navigation status panel compact and right-aligned.
        LinearLayout status = new LinearLayout(context);
        status.setOrientation(VERTICAL);
        status.setGravity(Gravity.CENTER);
        status.setBackground(panel(11));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                0, dp(103), 1f);
        row.addView(status, statusParams);

        clock = label(23, Typeface.BOLD);
        elevation = label(13, Typeface.NORMAL);
        settings = label(24, Typeface.BOLD);
        settings.setText("☰");
        settings.setContentDescription("Быстрые настройки карты");
        settings.setClickable(true);
        settings.setFocusable(true);
        settings.setOnClickListener(v -> {
            View nativeSettings = getRootView().findViewById(R.id.map_layers_button);
            if (nativeSettings != null) nativeSettings.performClick();
        });
        status.addView(clock, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, dp(36)));
        status.addView(settings, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, dp(32)));
        status.addView(elevation, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, dp(30)));

        turns.setVisibility(INVISIBLE);
        routeHeader.setVisibility(GONE);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private TextView label(int sp, int weight) {
        TextView v = new TextView(getContext());
        v.setTextColor(Color.WHITE);
        v.setTextSize(sp);
        v.setTypeface(Typeface.DEFAULT, weight);
        v.setGravity(Gravity.CENTER);
        v.setIncludeFontPadding(false);
        return v;
    }

    private GradientDrawable panel(int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF5000000);
        bg.setCornerRadius(dp(radius));
        bg.setStroke(dp(1), 0xFFDDDDDD);
        return bg;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.removeCallbacks(update);
        update.run();
    }

    @Override protected void onDetachedFromWindow() {
        handler.removeCallbacks(update);
        restoreNativeWidgets();
        super.onDetachedFromWindow();
    }

    private String distance(int meters) {
        int value = Math.max(0, meters);
        if (value < 1000) return value + " м";
        if (value < 10000) return String.format(Locale.getDefault(), "%.1f км", value / 1000.0);
        return String.format(Locale.getDefault(), "%.0f км", value / 1000.0);
    }

    private void refresh() {
        clock.setText(DateFormat.format("HH:mm", System.currentTimeMillis()));
        Location location = app.getLocationProvider().getLastKnownLocation();
        elevation.setText(location != null && location.hasAltitude()
                ? Math.round(location.getAltitude()) + " м" : "— м");

        RoutingHelper routing = app.getRoutingHelper();
        boolean active = routing != null && routing.isFollowingMode()
                && routing.isRouteCalculated();
        setNativeWidgetsHidden(active);
        turns.setVisibility(active ? VISIBLE : INVISIBLE);
        if (!active) {
            routeHeader.setVisibility(GONE);
            return;
        }

        NextDirectionInfo next = routing.getNextRouteDirectionInfo(
                new NextDirectionInfo(), false);
        if (next == null || next.directionInfo == null) {
            primaryDistance.setText("");
            secondaryDistance.setText("");
            primaryIcon.setVisibility(INVISIBLE);
            secondaryIcon.setVisibility(INVISIBLE);
            routeHeader.setVisibility(GONE);
            return;
        }

        primaryIcon.setVisibility(VISIBLE);
        primaryDrawable.setTurnType(next.directionInfo.getTurnType());
        primaryDrawable.invalidateSelf();
        primaryIcon.invalidate();
        primaryDistance.setText(distance(next.distanceTo));

        NextDirectionInfo second = routing.getNextRouteDirectionInfoAfter(
                next, new NextDirectionInfo(), false);
        boolean hasSecond = second != null && second.directionInfo != null;
        secondaryIcon.setVisibility(hasSecond ? VISIBLE : INVISIBLE);
        secondaryDistance.setVisibility(hasSecond ? VISIBLE : INVISIBLE);
        if (hasSecond) {
            secondaryDrawable.setTurnType(second.directionInfo.getTurnType());
            secondaryDrawable.invalidateSelf();
            secondaryIcon.invalidate();
            secondaryDistance.setText(distance(second.distanceTo));
        }

        CurrentStreetName name = routing.getCurrentName(next, false);
        String realName = name != null ? name.text : null;
        boolean hasName = !TextUtils.isEmpty(realName);
        routeHeader.setVisibility(hasName ? VISIBLE : GONE);
        if (hasName) routeText.setText("• " + realName);
    }

    private void setNativeWidgetsHidden(boolean active) {
        if (!active || getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_PORTRAIT) {
            restoreNativeWidgets();
            return;
        }
        // The screen in the reference replaces the default routing widgets,
        // not the map itself. Keep original views available for reuse on exit.
        for (String name : new String[]{
                "top_widgets_panel",
                "map_left_widgets_panel",
                "map_right_widgets_panel",
                "map_bottom_widgets_panel",
                "map_menu_button"}) {
            int id = getResources().getIdentifier(
                    name, "id", getContext().getPackageName());
            if (id == 0) continue;
            View nativeView = getRootView().findViewById(id);
            if (nativeView == null || nativeView == this) continue;
            if (!nativeVisibility.containsKey(nativeView))
                nativeVisibility.put(nativeView, nativeView.getVisibility());
            nativeView.setVisibility(INVISIBLE);
        }
    }

    private void restoreNativeWidgets() {
        for (Map.Entry<View, Integer> entry : nativeVisibility.entrySet()) {
            entry.getKey().setVisibility(entry.getValue());
        }
        nativeVisibility.clear();
    }
}
