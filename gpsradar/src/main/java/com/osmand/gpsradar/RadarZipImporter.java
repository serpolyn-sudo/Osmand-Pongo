package com.osmand.gpsradar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Strict bounded parser for the 47 country TXT files inside a user-requested
 * car.zip / truck.zip. Supports iGO X,Y,TYPE,SPEED,DIRTYPE,DIRECTION headers.
 * Unsupported files are rejected instead of silently producing fake POIs.
 */
public final class RadarZipImporter {
    public static final int MAX_POINTS = 2_000_000;
    public static final int MAX_TXT_FILES = 128;
    public static final long MAX_UNPACKED_BYTES = 180L * 1024 * 1024;

    private RadarZipImporter() {}

    public static List<RadarCamera> parse(InputStream source) throws IOException {
        List<RadarCamera> result = new ArrayList<>();
        int files = 0;
        long unpacked = 0;
        try (ZipInputStream zip = new ZipInputStream(source, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (name.startsWith("/") || name.contains("..") || name.contains("\\")) {
                    throw new IOException("Unsafe ZIP filename");
                }
                if (!name.toLowerCase(Locale.ROOT).endsWith(".txt")) continue;
                if (++files > MAX_TXT_FILES) throw new IOException("Too many country TXT files");
                String country = name.substring(name.lastIndexOf('/') + 1)
                        .replaceAll("(?i)\\.txt$", "");
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(zip, StandardCharsets.UTF_8));
                int[] columns = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    unpacked += line.length() * 4L + 2;
                    if (unpacked > MAX_UNPACKED_BYTES) throw new IOException("ZIP too large");
                    if (line.length() > 8192) throw new IOException("TXT record too long");
                    String s = line.trim();
                    if (s.isEmpty() || s.startsWith("#") || s.startsWith(";")) continue;
                    String[] cells = s.split(",", -1);
                    if (columns == null) {
                        if (s.toUpperCase(Locale.ROOT).contains("TYPE")
                                && s.toUpperCase(Locale.ROOT).contains("X")) {
                            columns = header(cells);
                            continue;
                        }
                        columns = new int[]{0,1,2,3,4,5};
                    }
                    if (cells.length <= Math.max(columns[2],Math.max(columns[0],columns[1]))) continue;
                    try {
                        double lon = Double.parseDouble(cell(cells, columns[0]));
                        double lat = Double.parseDouble(cell(cells, columns[1]));
                        int type = Integer.parseInt(cell(cells, columns[2]));
                        int speed = optionalInt(cells, columns[3]);
                        int dirType = optionalInt(cells, columns[4]);
                        double bearing = optionalDouble(cells, columns[5]);
                        if (type >= 32 && type <= 63) {
                            result.add(new RadarCamera(lat, lon, type, speed, dirType,
                                    bearing, country));
                        }
                    } catch (NumberFormatException | IllegalArgumentException invalidRecord) {
                        // Not a camera record: ignore, but never invent coordinates.
                    }
                    if (result.size() > MAX_POINTS) throw new IOException("Too many objects");
                }
                zip.closeEntry();
            }
        }
        if (files == 0 || result.isEmpty()) throw new IOException(
                "No valid GPSRadar camera records; inspect actual TXT format");
        return result;
    }

    private static int[] header(String[] fields) {
        int[] positions = {-1,-1,-1,-1,-1,-1};
        String[] names = {"X","Y","TYPE","SPEED","DIRTYPE","DIRECTION"};
        for (int j = 0; j < names.length; j++) {
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].trim().equalsIgnoreCase(names[j])) {
                    positions[j] = i;
                    break;
                }
            }
        }
        if (positions[0] < 0 || positions[1] < 0 || positions[2] < 0)
            throw new IllegalArgumentException("Missing X,Y,TYPE columns");
        return positions;
    }

    private static String cell(String[] fields, int at) {
        return at >= 0 && at < fields.length ? fields[at].trim() : "";
    }
    private static int optionalInt(String[] cells, int at) {
        String s = cell(cells, at);
        return s.isEmpty() ? 0 : Integer.parseInt(s);
    }
    private static double optionalDouble(String[] cells, int at) {
        String s = cell(cells, at);
        return s.isEmpty() ? Double.NaN : Double.parseDouble(s);
    }
}
