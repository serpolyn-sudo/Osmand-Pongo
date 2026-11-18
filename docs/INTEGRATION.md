# OsmAnd GPSRadar — integration specification

## Clean baseline
Use fresh original osmandapp/OsmAnd Android source. No Pongo interfaces, branding or overlays.
Keep all original routing, widgets, map, menus and user profiles.

## Profile isolation
- Truck profile: truck.zip.
- Car, Motorcycle and Bus profiles: car.zip.
- Other profiles: car.zip until explicitly configured.
- Save warning mode, TYPE filters, user distances, overspeed margin,
  sound choices, marker visibility and widget position per OsmAnd profile.
- Available modes: Stock and Extended. Stock uses existing warnings;
  Extended replaces overlapping stock camera alerts with GPSRadar TYPE 32-63.

## Downloads
In Maps & resources > Downloads place Cameras speed-control row ABOVE Marine maps.
Click opens Europe; then Car and Truck download entries.
Use manually triggered HTTPS ZIP fetch from slagboom.narod.ru/gpsradar/.
Use manifest.json to show real archive version and metadata when known.
Never download automatically, at startup or in a scheduled job.
A correct UX shows disabled/unavailable status if an archive is missing.

## POI map
All categories TYPE 32..63 always visible on map using compact style markers
when a valid database has been imported and profile marker visibility is on.
Icons are grouped by shape and distinguished by their TYPE number.
Use zoom-level clustering/spatial index for large country sets.

## Warnings and widget
Display camera or road hazard name and actual distance ahead.
If limit known, show speed limit and warn when current speed exceeds
limit + the configured per-profile tolerance.
For average-speed control type 35 display measured average ONLY if a
confirmed start/end section and elapsed travel measurements are available.
No imaginary average values. Same actual distance for speech and UI.
The widget must be a normal OsmAnd native registered widget with user-selectable
left/right/top/bottom panel placement and normal appearance settings.
If no active warning the content may collapse.
Default trigger distances: 700, 300 and 150 meters; all editable.

## Required upstream API wiring, not yet implemented
- WidgetType, MapWidgetsFactory, WidgetInfoCreator, WidgetsInitializer:
  native widget registration and profile panel preference.
- DownloadResources, DownloadResourceGroup, adapter:
  Europe camera download rows and manual buttons above nautical maps.
- OsmAnd map point layer and spatial index for icons;
  routing map-matching, voice manager and average speed section manager;
  per-profile settings persistence and stock alert coordination.
- Database archive importer must be checked against original country TXT
  structure before treating parsed POI as verified.
- Build must verify Gradle, install and test on both portrait and landscape.

## Build policy
Do NOT invoke compilation or GitHub Actions without the user's later request.
This is an uncompiled, partial foundation, not a ready-to-install APK.
