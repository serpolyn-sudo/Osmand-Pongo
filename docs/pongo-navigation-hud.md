# OsmAnd-Pongo navigation HUD specification

Reference: user's iGO Pongo navigation screenshots, particularly the landscape navigation view. This is a functional design target, not a claim that these widgets already exist.

## Scope
- Separate Android package `com.osmand.pongo`, retaining existing OsmAnd installation.
- Same navigation HUD layout for both passenger-car and truck profiles; profile-dependent restrictions can be added later.
- Show route-dependent widgets only when turn-by-turn navigation is active. Do not display fabricated navigation values.
- Preserve map interaction, guidance, and existing OsmAnd routing.

## Screen positions
1. **Upper left:** large rounded first-turn panel with direction arrow and remaining distance to the next maneuver. Small adjacent panel indicates second maneuver and its distance when known.
2. **Top center:** compact route shields, road identifiers and destination/city directional label when supplied by routing/map data.
3. **Upper horizon:** road sign / POI / restrictions strip as available from reliable map data; do not claim dynamic information not in the engine.
4. **Upper right:** clock and current elevation above mean sea level, plus a three-line button opening quick settings. Height is a measured/derived reading and should be absent when unavailable.
5. **Lower left above trip panel:** actual GPS speed in km/h and mapped/legal speed limit on the current road (unknown limit shown as unknown). Road signs follow passenger/truck mode where data supports it.
6. **Lower left trip panel, only during navigation:** arrival clock time, remaining travel duration, remaining route distance, in that order.
7. **Bottom center:** current street name or road identifier.
8. **Lower right:** battery charge percentage and three-line button opening the main menu.
9. **Map center:** clear route-aware map with navigation perspective; no PONGO NAV branding overlay; do not cover the driving direction with opaque panels.

## Engineering constraints / acceptance
- Route widgets must subscribe to real OsmAnd navigation state and update upon recalc, reroute, GPS change and navigation stop.
- Menu buttons must be connected to functioning destinations, not visually simulated.
- Reuse native map turn, ETA, speed-limit, location and battery data where possible; evaluate what requires Java/Kotlin integration.
- Treat iGO screenshots as a layout/style reference; do not copy iGO code/assets.
- Support landscape tablet sizes and portrait phone layout responsively.
- Check that separate APK can install alongside original, open map, start/end a route, and show no fictitious values.
- No full implementation or validation is implied by this design file.

## Confirmed menu and speed-control decisions (18 Nov 2026)
- Bottom-right combined battery-percentage / three-horizontal-lines control is the **main menu button**. Its action must open the **existing OsmAnd main menu unchanged**; do not redesign the menu yet.
- The upper-right quick-settings control is distinct from the main menu button.
- Bottom-left current speed and road speed-limit sign must sit adjacent, including examples such as 140 km/h only when real map/routing data indicates that limit.
- Focus current implementation on the **navigation foreground/map HUD** and defer menu/settings redesign.

## Orientation-specific layout (approved 18 Nov 2026)
- **Landscape:** retain the previously approved landscape Pongo HUD layout as-is. Do not apply portrait-only sizing or positions to landscape.
- **Portrait:** follow the supplied iGO Pongo portrait screenshot. Top-left first/second maneuver widgets; top-right time, elevation and separate quick settings; keep the central 3D navigation map open. Bottom-left speed limit above current GPS speed, bottom road-name strip with route shields and ETA, duration, remaining distance. Bottom-right battery percentage plus a button opening the current (unchanged) OsmAnd main menu.
- Native Android `res/layout-port/` overrides are used to make portrait widget containers independent of landscape. The first committed implementation moves real native speed-limit above speedometer and adjusts the portrait top panel insets; it does **not** yet implement the full screenshot-style HUD, route banner, battery-menu hybrid control, or all route readouts.
- Every data value must come from OsmAnd live navigation, GPS or system state; don't hardcode the values appearing in the reference picture.
- **Do not trigger APK builds until layout preview has been reviewed.** The implementation is prepared on `feature/portrait-pongo-hud`, distinct from `main`. A later merge to `main` may trigger automatic Actions runs because of workflow path triggers.
