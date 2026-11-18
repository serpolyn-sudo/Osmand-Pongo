package net.osmand.plus.views.controls;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.CurrentStreetName;
import net.osmand.plus.routing.NextDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;

import java.util.Locale;

/**
 * Portrait-only Pongo trip footer. All navigation values come directly from
 * RoutingHelper. Never insert demonstration mileage, road names or ETA.
 */
public class PongoRouteBar extends LinearLayout {

    private final OsmandApplication app;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TextView street;
    private final TextView eta;
    private final TextView duration;
    private final TextView distance;
    private final TextView menu;
    private final LinearLayout tripRow;
    private final Runnable update = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, 1500);
        }
    };

    public PongoRouteBar(Context context) {
        this(context, null);
    }

    public PongoRouteBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        app = (OsmandApplication) context.getApplicationContext();
        setOrientation(VERTICAL);
        setClickable(false);
        setClipChildren(false);
        setPadding(dp(4), 0, dp(4), dp(4));

        street = label(15, Typeface.BOLD);
        street.setGravity(Gravity.CENTER_VERTICAL);
        street.setSingleLine(true);
        street.setEllipsize(TextUtils.TruncateAt.END);
        street.setBackground(panel());
        street.setPadding(dp(12), dp(7), dp(8), dp(7));
        LinearLayout.LayoutParams roadParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, dp(41));
        roadParams.bottomMargin = dp(3);
        addView(street, roadParams);

        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(HORIZONTAL);
        addView(controls, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, dp(52)));

        tripRow = new LinearLayout(context);
        tripRow.setOrientation(HORIZONTAL);
        tripRow.setGravity(Gravity.CENTER_VERTICAL);
        tripRow.setBackground(panel());
        tripRow.setPadding(dp(5), 0, dp(5), 0);
        LinearLayout.LayoutParams tripParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
        tripParams.rightMargin = dp(4);
        controls.addView(tripRow, tripParams);

        eta = label(14, Typeface.BOLD);
        duration = label(14, Typeface.BOLD);
        distance = label(14, Typeface.BOLD);
        tripRow.addView(eta, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
        tripRow.addView(duration, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
        tripRow.addView(distance, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));

        menu = label(14, Typeface.BOLD);
        menu.setGravity(Gravity.CENTER);
        menu.setBackground(panel());
        menu.setContentDescription("Заряд батареи и главное меню");
        menu.setClickable(true);
        menu.setFocusable(true);
        controls.addView(menu, new LinearLayout.LayoutParams(dp(79), LayoutParams.MATCH_PARENT));
        menu.setOnClickListener(v -> openExistingMenu());
        refresh();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(int sp, int weight) {
        TextView view = new TextView(getContext());
        view.setTextColor(Color.WHITE);
        view.setTextSize(sp);
        view.setTypeface(Typeface.DEFAULT, weight);
        view.setGravity(Gravity.CENTER);
        view.setIncludeFontPadding(false);
        return view;
    }

    private GradientDrawable panel() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF0000000);
        bg.setCornerRadius(dp(11));
        bg.setStroke(dp(1), 0xFFD8D8D8);
        return bg;
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

    private void refresh() {
        Intent battery = getContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int percent = -1;
        if (battery != null) {
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) percent = Math.round(100f * level / scale);
        }
        menu.setText((percent < 0 ? "—" : percent + "%") + "\n☰");

        RoutingHelper routing = app.getRoutingHelper();
        boolean active = routing != null && routing.isFollowingMode()
                && routing.isRouteCalculated();
        street.setVisibility(active ? VISIBLE : GONE);
        tripRow.setVisibility(active ? VISIBLE : INVISIBLE);
        if (!active) return;

        int seconds = Math.max(0, routing.getLeftTime());
        int meters = Math.max(0, routing.getLeftDistance());
        eta.setText(DateFormat.format("HH:mm", System.currentTimeMillis() + seconds * 1000L));
        duration.setText(String.format(Locale.getDefault(), "%d:%02d", seconds / 3600, (seconds / 60) % 60));
        distance.setText(meters < 1000 ? meters + " м"
                : String.format(Locale.getDefault(), "%.0f км", meters / 1000.0));

        NextDirectionInfo next = routing.getNextRouteDirectionInfo(new NextDirectionInfo(), false);
        if (next != null) {
            CurrentStreetName road = routing.getCurrentName(next, false);
            street.setText(road != null && !TextUtils.isEmpty(road.text) ? road.text : " ");
        } else {
            street.setText(" ");
        }
    }

    private void openExistingMenu() {
        View nativeButton = getRootView().findViewById(R.id.map_menu_button);
        if (nativeButton != null) {
            nativeButton.performClick();
            return;
        }
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof MapActivity) {
                ((MapActivity) context).openDrawer();
                return;
            }
            Context base = ((ContextWrapper) context).getBaseContext();
            if (base == context) break;
            context = base;
        }
    }
}
